package com.example.liber.feature.dictionary

import android.app.Application
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.liber.core.util.UiState
import com.example.liber.core.util.UiText
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
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class DictionaryViewModel @Inject constructor(
    application: Application,
    private val dictionaryRepository: DictionaryRepository,
) : AndroidViewModel(application) {

    private val _freeDictCatalogState =
        MutableStateFlow<UiState<List<FreeDictCatalogItem>>>(UiState.Loading)
    val freeDictCatalogState: StateFlow<UiState<List<FreeDictCatalogItem>>> =
        _freeDictCatalogState.asStateFlow()

    private val _downloadingCodes = MutableStateFlow<Set<String>>(emptySet())
    val downloadingCodes: StateFlow<Set<String>> = _downloadingCodes.asStateFlow()

    private val _activeLookupQuery = MutableStateFlow<String?>(null)
    val activeLookupQuery: StateFlow<String?> = _activeLookupQuery

    private val _lookupState = MutableStateFlow<UiState<List<DictionaryEntryWithSenses>>>(
        UiState.Success(emptyList())
    )
    val lookupState: StateFlow<UiState<List<DictionaryEntryWithSenses>>> = _lookupState

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
        viewModelScope.launch(Dispatchers.IO) {
            val name = displayName.trim()
            if (name.isBlank()) return@launch
            dictionaryRepository.createManualDictionary(
                displayName = name,
                sourceLanguageTag = sourceLanguageTag.trim().ifEmpty { "en" },
                targetLanguageTag = targetLanguageTag?.trim()?.ifEmpty { null },
                dictionaryType = dictionaryType,
                uri = uri,
            )
        }
    }

    fun renameDictionary(id: String, alias: String?) {
        viewModelScope.launch(Dispatchers.IO) {
            dictionaryRepository.renameDictionary(id, alias)
        }
    }

    fun setDictionaryEnabled(id: String, enabled: Boolean) {
        viewModelScope.launch(Dispatchers.IO) {
            dictionaryRepository.setDictionaryEnabled(id, enabled)
        }
    }

    fun moveDictionaryPriority(id: String, moveUp: Boolean) {
        viewModelScope.launch(Dispatchers.IO) {
            val current = dictionaries.value
            val index = current.indexOfFirst { it.id == id }
            if (index < 0) return@launch

            val targetIndex = if (moveUp) index - 1 else index + 1
            if (targetIndex !in current.indices) return@launch

            val source = current[index]
            val target = current[targetIndex]
            dictionaryRepository.setDictionaryPriority(source.id, target.priority)
            dictionaryRepository.setDictionaryPriority(target.id, source.priority)
        }
    }

    fun deleteDictionary(id: String) {
        viewModelScope.launch(Dispatchers.IO) {
            dictionaryRepository.deleteDictionary(id)
        }
    }

    fun clearLookupHistory() {
        viewModelScope.launch(Dispatchers.IO) {
            dictionaryRepository.clearLookupHistory()
        }
    }

    fun refreshFreeDictCatalog() {
        _freeDictCatalogState.value = UiState.Loading
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val catalog = dictionaryRepository.fetchFreeDictCatalog()
                _freeDictCatalogState.value = UiState.Success(catalog)
            } catch (e: Exception) {
                _freeDictCatalogState.value = UiState.Error(
                    UiText.DynamicString(e.message ?: "Failed to load FreeDict catalog")
                )
            }
        }
    }

    fun downloadFreeDict(item: FreeDictCatalogItem) {
        if (_downloadingCodes.value.contains(item.code)) return

        _downloadingCodes.value = _downloadingCodes.value + item.code
        viewModelScope.launch(Dispatchers.IO) {
            try {
                dictionaryRepository.downloadAndInstallFreeDict(item)
            } finally {
                _downloadingCodes.value = _downloadingCodes.value - item.code
            }
        }
    }

    fun lookupWord(query: String, languageTag: String, sourceBookId: String?) {
        val trimmed = query.trim()
        if (trimmed.isBlank()) {
            _activeLookupQuery.value = null
            _lookupState.value = UiState.Success(emptyList())
            return
        }

        _activeLookupQuery.value = trimmed
        _lookupState.value = UiState.Loading
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val results = dictionaryRepository.searchEntries(trimmed, languageTag)
                _lookupState.value = UiState.Success(results)

                val first = results.firstOrNull()
                dictionaryRepository.addLookupHistory(
                    query = trimmed,
                    entryId = first?.entry?.id,
                    dictionaryId = first?.entry?.dictionaryId,
                    sourceBookId = sourceBookId,
                )
            } catch (e: Exception) {
                _lookupState.value = UiState.Error(
                    UiText.DynamicString(
                        e.message ?: "Dictionary lookup failed"
                    )
                )
            }
        }
    }

    fun clearLookupState() {
        _activeLookupQuery.value = null
        _lookupState.value = UiState.Success(emptyList())
    }

    fun deleteLookupHistory(id: Long) {
        viewModelScope.launch(Dispatchers.IO) {
            dictionaryRepository.deleteLookupHistory(id)
        }
    }
}
