package com.example.liber.feature.dictionary

import android.app.Application
import android.net.Uri
import androidx.lifecycle.viewModelScope
import com.example.liber.core.error.toAppError
import com.example.liber.core.error.toUiStateError
import com.example.liber.core.logging.AppLogger
import com.example.liber.core.logging.BaseAndroidViewModel
import com.example.liber.core.util.UiState
import com.example.liber.core.util.UiText
import com.example.liber.core.util.rethrowIfCancellation
import com.example.liber.data.local.DictionaryEntryWithSenses
import com.example.liber.data.model.Dictionary
import com.example.liber.data.model.DictionaryLookupHistory
import com.example.liber.data.model.FreeDictCatalogItem
import com.example.liber.data.repository.DictionaryRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import javax.inject.Inject

@HiltViewModel
class DictionaryViewModel @Inject constructor(
    application: Application,
    private val dictionaryRepository: DictionaryRepository,
    private val appLogger: AppLogger,
) : BaseAndroidViewModel(application, "DictionaryViewModel", appLogger) {

    private val _freeDictCatalogState =
        MutableStateFlow<UiState<List<FreeDictCatalogItem>>>(UiState.Loading)
    val freeDictCatalogState: StateFlow<UiState<List<FreeDictCatalogItem>>> =
        _freeDictCatalogState.asStateFlow()

    private val _downloadingCodes = MutableStateFlow<Set<String>>(emptySet())
    val downloadingCodes: StateFlow<Set<String>> = _downloadingCodes.asStateFlow()

    val lemmatizationStatus: StateFlow<Map<String, String>> = dictionaryRepository.lemmatizationStatus

    val languagesWithLemmas: StateFlow<Set<String>> = dictionaryRepository.languagesWithLemmas
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptySet())

    fun normalizeLanguageTag(tag: String): String = dictionaryRepository.normalizeLanguageTag(tag)

    private val _activeLookupQuery = MutableStateFlow<String?>(null)
    val activeLookupQuery: StateFlow<String?> = _activeLookupQuery

    private val _lookupState = MutableStateFlow<UiState<List<DictionaryEntryWithSenses>>>(
        UiState.Success(emptyList())
    )
    val lookupState: StateFlow<UiState<List<DictionaryEntryWithSenses>>> = _lookupState

    private val _browseState = MutableStateFlow<UiState<List<DictionaryEntryWithSenses>>>(
        UiState.Success(emptyList())
    )
    val browseState: StateFlow<UiState<List<DictionaryEntryWithSenses>>> = _browseState

    private val _browseQuery = MutableStateFlow("")
    val browseQuery: StateFlow<String> = _browseQuery

    private var currentBrowseDictionaryId: String? = null
    private var browseOffset = 0
    private var isEndReached = false
    private val PAGE_SIZE = 50

    private val _isBrowsingMore = MutableStateFlow(false)
    val isBrowsingMore: StateFlow<Boolean> = _isBrowsingMore

    val dictionariesState: StateFlow<UiState<List<Dictionary>>> = dictionaryRepository
        .getAllDictionaries()
        .map { UiState.Success(it) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), UiState.Loading)

    val dictionaries: StateFlow<List<Dictionary>> = dictionariesState
        .map { (it as? UiState.Success)?.data ?: emptyList() }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val lookupHistory: StateFlow<List<DictionaryLookupHistory>> = dictionaryRepository
        .getLookupHistory()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    init {
        refreshFreeDictCatalog()
    }

    fun createDictionary(
        displayName: String,
        sourceLanguageTag: String,
        targetLanguageTag: String?,
        dictionaryType: String,
        uri: Uri?,
    ) {
        launchSafely(
            actionName = "createDictionary",
            dispatcher = Dispatchers.IO,
            parameters = mapOf(
                "displayName" to displayName.trim(),
                "sourceLanguageTag" to sourceLanguageTag,
                "targetLanguageTag" to targetLanguageTag,
                "dictionaryType" to dictionaryType,
                "hasUri" to (uri != null),
            ),
        ) {
            val name = displayName.trim()
            if (name.isBlank()) return@launchSafely
            runCatching {
                dictionaryRepository.createManualDictionary(
                    displayName = name,
                    sourceLanguageTag = sourceLanguageTag.trim().ifEmpty { "en" },
                    targetLanguageTag = targetLanguageTag?.trim()?.ifEmpty { null },
                    dictionaryType = dictionaryType,
                    uri = uri,
                )
            }.onFailure { throwable ->
                throwable.rethrowIfCancellation()
                logger.recordError(throwable, "createDictionary", "Failed to create dictionary")
            }
        }
    }

    fun renameDictionary(id: String, alias: String?) {
        launchSafely(
            actionName = "renameDictionary",
            dispatcher = Dispatchers.IO,
            parameters = mapOf("id" to id, "hasAlias" to !alias.isNullOrBlank()),
        ) {
            dictionaryRepository.renameDictionary(id, alias)
        }
    }

    fun setDictionaryEnabled(id: String, enabled: Boolean) {
        launchSafely(
            actionName = "setDictionaryEnabled",
            dispatcher = Dispatchers.IO,
            parameters = mapOf("id" to id, "enabled" to enabled),
        ) {
            dictionaryRepository.setDictionaryEnabled(id, enabled)
        }
    }

    fun moveDictionaryPriority(id: String, moveUp: Boolean) {
        launchSafely(
            actionName = "moveDictionaryPriority",
            dispatcher = Dispatchers.IO,
            parameters = mapOf("id" to id, "moveUp" to moveUp),
        ) {
            val current = dictionaries.value
            val index = current.indexOfFirst { it.id == id }
            if (index < 0) return@launchSafely

            val targetIndex = if (moveUp) index - 1 else index + 1
            if (targetIndex !in current.indices) return@launchSafely

            val source = current[index]
            val target = current[targetIndex]
            dictionaryRepository.setDictionaryPriority(source.id, target.priority)
            dictionaryRepository.setDictionaryPriority(target.id, source.priority)
        }
    }

    fun deleteDictionary(id: String) {
        launchSafely(
            actionName = "deleteDictionary",
            dispatcher = Dispatchers.IO,
            parameters = mapOf("id" to id),
        ) {
            dictionaryRepository.deleteDictionary(id)
        }
    }

    fun clearLookupHistory() {
        launchSafely(
            actionName = "clearLookupHistory",
            dispatcher = Dispatchers.IO,
        ) {
            dictionaryRepository.clearLookupHistory()
        }
    }

    fun refreshFreeDictCatalog() {
        _freeDictCatalogState.value = UiState.Loading
        launchSafely(
            actionName = "refreshFreeDictCatalog",
            dispatcher = Dispatchers.IO,
            onError = { throwable ->
                _freeDictCatalogState.value = throwable.toAppError().toUiStateError()
            },
        ) {
            try {
                val catalog = dictionaryRepository.fetchFreeDictCatalog()
                _freeDictCatalogState.value = UiState.Success(catalog)
            } catch (e: Exception) {
                e.rethrowIfCancellation()
                logger.recordError(e, "refreshFreeDictCatalog", "Failed to refresh FreeDict catalog")
                _freeDictCatalogState.value = e.toAppError().toUiStateError()
            }
        }
    }

    fun downloadFreeDict(item: FreeDictCatalogItem) {
        if (_downloadingCodes.value.contains(item.code)) return

        _downloadingCodes.value = _downloadingCodes.value + item.code
        launchSafely(
            actionName = "downloadFreeDict",
            dispatcher = Dispatchers.IO,
            parameters = mapOf("code" to item.code, "version" to item.version),
        ) {
            try {
                dictionaryRepository.downloadAndInstallFreeDict(item)
            } catch (e: Exception) {
                e.rethrowIfCancellation()
                logger.recordError(e, "downloadFreeDict", "Failed to download FreeDict ${item.code}")
            } finally {
                _downloadingCodes.value = _downloadingCodes.value - item.code
            }
        }
    }

    fun lookupWord(query: String, languageTag: String, sourceBookId: String?) {
        val trimmed = query.trim()
        logger.log("lookupWord: \"$trimmed\" ($languageTag)")
        if (trimmed.isBlank()) {
            _activeLookupQuery.value = null
            _lookupState.value = UiState.Success(emptyList())
            return
        }

        _activeLookupQuery.value = trimmed
        _lookupState.value = UiState.Loading
        launchSafely(
            actionName = "lookupWord",
            dispatcher = Dispatchers.IO,
            parameters = mapOf(
                "query" to trimmed,
                "languageTag" to languageTag,
                "sourceBookId" to sourceBookId,
            ),
            onError = { throwable ->
                _lookupState.value = throwable.toAppError().toUiStateError()
            },
        ) {
            try {
                val results = dictionaryRepository.searchEntries(trimmed, languageTag)
                logger.log("lookupWord: found ${results.size} results")
                _lookupState.value = UiState.Success(results)

                val first = results.firstOrNull()
                dictionaryRepository.addLookupHistory(
                    query = trimmed,
                    entryId = first?.entry?.id,
                    dictionaryId = first?.entry?.dictionaryId,
                    sourceBookId = sourceBookId,
                )
            } catch (e: Exception) {
                e.rethrowIfCancellation()
                logger.recordError(e, "lookupWord", "lookupWord failed")
                _lookupState.value = e.toAppError().toUiStateError()
            }
        }
    }

    fun clearLookupState() {
        _activeLookupQuery.value = null
        _lookupState.value = UiState.Success(emptyList())
    }

    fun deleteLookupHistory(id: Long) {
        launchSafely(
            actionName = "deleteLookupHistory",
            dispatcher = Dispatchers.IO,
            parameters = mapOf("id" to id),
        ) {
            dictionaryRepository.deleteLookupHistory(id)
        }
    }

    fun browseDictionary(dictionaryId: String, query: String = "") {
        if (currentBrowseDictionaryId == dictionaryId && _browseQuery.value == query && _browseState.value !is UiState.Error) {
            return
        }

        currentBrowseDictionaryId = dictionaryId
        _browseQuery.value = query
        _browseState.value = UiState.Loading
        browseOffset = 0
        isEndReached = false

        launchSafely(
            actionName = "browseDictionary",
            dispatcher = Dispatchers.IO,
            parameters = mapOf("dictionaryId" to dictionaryId, "query" to query),
            onError = { throwable ->
                _browseState.value = throwable.toAppError().toUiStateError()
            }
        ) {
            val results = if (query.isBlank()) {
                dictionaryRepository.getEntriesByDictionary(dictionaryId, limit = PAGE_SIZE, offset = 0)
            } else {
                dictionaryRepository.searchEntriesInDictionary(dictionaryId, query, limit = PAGE_SIZE, offset = 0)
            }
            isEndReached = results.size < PAGE_SIZE
            browseOffset = results.size
            _browseState.value = UiState.Success(results)
        }
    }

    fun loadMoreEntries() {
        val dictId = currentBrowseDictionaryId ?: return
        if (isEndReached || _isBrowsingMore.value || _browseState.value !is UiState.Success) return

        val query = _browseQuery.value
        _isBrowsingMore.value = true

        launchSafely(
            actionName = "loadMoreEntries",
            dispatcher = Dispatchers.IO,
            parameters = mapOf("dictionaryId" to dictId, "query" to query, "offset" to browseOffset),
            onError = { _isBrowsingMore.value = false }
        ) {
            try {
                val results = if (query.isBlank()) {
                    dictionaryRepository.getEntriesByDictionary(dictId, limit = PAGE_SIZE, offset = browseOffset)
                } else {
                    dictionaryRepository.searchEntriesInDictionary(dictId, query, limit = PAGE_SIZE, offset = browseOffset)
                }

                isEndReached = results.size < PAGE_SIZE
                browseOffset += results.size

                val currentList = (_browseState.value as UiState.Success).data
                _browseState.value = UiState.Success(currentList + results)
            } finally {
                _isBrowsingMore.value = false
            }
        }
    }

    fun clearBrowseState() {
        currentBrowseDictionaryId = null
        browseOffset = 0
        isEndReached = false
        _browseQuery.value = ""
        _browseState.value = UiState.Success(emptyList())
    }
}
