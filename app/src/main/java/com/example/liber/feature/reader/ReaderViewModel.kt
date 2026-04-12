package com.example.liber.feature.reader

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.liber.data.repository.UserPreferencesRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import org.readium.r2.shared.ExperimentalReadiumApi
import org.readium.r2.shared.publication.Locator
import org.readium.r2.shared.publication.Publication
import org.readium.r2.shared.publication.services.positions
import org.readium.r2.shared.publication.services.search.SearchIterator
import org.readium.r2.shared.publication.services.search.search

// Default annotation color — yellow at 50 % opacity (visible on both light and dark themes)
private val DEFAULT_ANNOTATION_COLOR = 0x80FFD60A.toInt()

private const val DEFAULT_LINE_SPACING = 1.4f
private const val DEFAULT_CHAR_SPACING = 0.0f
private const val DEFAULT_WORD_SPACING = 0.0f
private const val DEFAULT_MARGINS = 0.0f

@OptIn(ExperimentalReadiumApi::class)
class ReaderViewModel(
    application: Application,
    val publication: Publication,
    private val userPreferencesRepository: UserPreferencesRepository
) : AndroidViewModel(application) {

    // ── Reader UI state ──────────────────────────────────────────────────────

    private val _showUI = MutableStateFlow(true)
    val showUI: StateFlow<Boolean> = _showUI

    fun toggleUI() {
        _showUI.value = !_showUI.value
    }

    // ── Theme & font size (persisted) ────────────────────────────────────────

    private val _themeId = MutableStateFlow("original")
    val themeId: StateFlow<String> = _themeId

    private val _fontSize = MutableStateFlow(1.0)
    val fontSize: StateFlow<Double> = _fontSize

    fun setTheme(id: String) {
        viewModelScope.launch {
            userPreferencesRepository.setReaderTheme(id)
        }
    }

    fun adjustFontSize(delta: Double) {
        viewModelScope.launch {
            val next = (_fontSize.value + delta).coerceIn(0.5, 2.0)
            userPreferencesRepository.setFontSize(next.toFloat())
        }
    }

    // ── Page flipping (scroll mode) ──────────────────────────────────────────

    private val _pageScroll = MutableStateFlow(false)
    val pageScroll: StateFlow<Boolean> = _pageScroll

    fun setPageScroll(scrollMode: Boolean) {
        viewModelScope.launch {
            userPreferencesRepository.setPageScroll(scrollMode)
        }
    }

    // ── Layout customization (persisted) ─────────────────────────────────────

    private val _customizeLayout = MutableStateFlow(false)
    val customizeLayout: StateFlow<Boolean> = _customizeLayout

    fun setCustomizeLayout(enabled: Boolean) {
        viewModelScope.launch {
            userPreferencesRepository.setCustomizeLayout(enabled)
        }
    }

    private val _lineSpacing = MutableStateFlow(DEFAULT_LINE_SPACING.toDouble())
    val lineSpacing: StateFlow<Double> = _lineSpacing

    fun setLineSpacing(value: Double) {
        viewModelScope.launch {
            userPreferencesRepository.setLineSpacing(value.toFloat())
        }
    }

    private val _characterSpacing = MutableStateFlow(DEFAULT_CHAR_SPACING.toDouble())
    val characterSpacing: StateFlow<Double> = _characterSpacing

    fun setCharacterSpacing(value: Double) {
        viewModelScope.launch {
            userPreferencesRepository.setCharSpacing(value.toFloat())
        }
    }

    private val _wordSpacing = MutableStateFlow(DEFAULT_WORD_SPACING.toDouble())
    val wordSpacing: StateFlow<Double> = _wordSpacing

    fun setWordSpacing(value: Double) {
        viewModelScope.launch {
            userPreferencesRepository.setWordSpacing(value.toFloat())
        }
    }

    private val _margins = MutableStateFlow(DEFAULT_MARGINS.toDouble())
    val margins: StateFlow<Double> = _margins

    fun setMargins(value: Double) {
        viewModelScope.launch {
            userPreferencesRepository.setMargins(value.toFloat())
        }
    }

    // "auto" | "one" | "two"
    private val _columnCount = MutableStateFlow("auto")
    val columnCount: StateFlow<String> = _columnCount

    fun setColumnCount(value: String) {
        viewModelScope.launch {
            userPreferencesRepository.setColumnCount(value)
        }
    }

    private val _justifyText = MutableStateFlow(false)
    val justifyText: StateFlow<Boolean> = _justifyText

    fun setJustifyText(value: Boolean) {
        viewModelScope.launch {
            userPreferencesRepository.setJustifyText(value)
        }
    }

    /** Resets all layout / typography settings to their defaults. */
    fun resetLayoutSettings() {
        viewModelScope.launch {
            userPreferencesRepository.resetReaderSettings()
        }
    }

    // ── Search ───────────────────────────────────────────────────────────────

    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery

    private val _searchResults = MutableStateFlow<List<Locator>>(emptyList())
    val searchResults: StateFlow<List<Locator>> = _searchResults

    private val _isSearching = MutableStateFlow(false)
    val isSearching: StateFlow<Boolean> = _isSearching

    private val _positions = MutableStateFlow<List<Locator>>(emptyList())

    init {
        viewModelScope.launch {
            _positions.value = publication.positions()
        }

        // Load preferences asynchronously from DataStore
        viewModelScope.launch {
            launch {
                userPreferencesRepository.readerTheme.collectLatest { _themeId.value = it }
            }
            launch {
                userPreferencesRepository.fontSize.collectLatest { _fontSize.value = it.toDouble() }
            }
            launch {
                userPreferencesRepository.pageScroll.collectLatest { _pageScroll.value = it }
            }
            launch {
                userPreferencesRepository.customizeLayout.collectLatest {
                    _customizeLayout.value = it
                }
            }
            launch {
                userPreferencesRepository.lineSpacing.collectLatest {
                    _lineSpacing.value = it.toDouble()
                }
            }
            launch {
                userPreferencesRepository.charSpacing.collectLatest {
                    _characterSpacing.value = it.toDouble()
                }
            }
            launch {
                userPreferencesRepository.wordSpacing.collectLatest {
                    _wordSpacing.value = it.toDouble()
                }
            }
            launch {
                userPreferencesRepository.margins.collectLatest { _margins.value = it.toDouble() }
            }
            launch {
                userPreferencesRepository.columnCount.collectLatest { _columnCount.value = it }
            }
            launch {
                userPreferencesRepository.justifyText.collectLatest { _justifyText.value = it }
            }
        }
    }

    fun locatorAtProgress(progress: Double): Locator? {
        val posList = _positions.value
        if (posList.isEmpty()) return null

        // Find the locator with the closest totalProgression
        return posList.minByOrNull { locator ->
            kotlin.math.abs((locator.locations.totalProgression ?: 0.0) - progress)
        }
    }

    private var searchIterator: SearchIterator? = null

    fun search(query: String) {
        _searchQuery.value = query
        searchIterator?.close()
        searchIterator = null
        _searchResults.value = emptyList()

        if (query.isBlank()) return

        _isSearching.value = true
        viewModelScope.launch {
            val iterator = publication.search(query)
            if (iterator != null) {
                searchIterator = iterator
                loadNextResults()
            }
            _isSearching.value = false
        }
    }

    fun loadNextResults() {
        val iterator = searchIterator ?: return
        viewModelScope.launch {
            iterator.next()
                .onSuccess { result ->
                    val newLocators = result?.locators ?: emptyList()
                    _searchResults.value = _searchResults.value + newLocators
                }
        }
    }

    // ── Annotation creation state ────────────────────────────────────────────

    private val _showAnnotationCreator = MutableStateFlow(false)
    val showAnnotationCreator: StateFlow<Boolean> = _showAnnotationCreator

    private val _pendingSelectedText = MutableStateFlow<String?>(null)
    val pendingSelectedText: StateFlow<String?> = _pendingSelectedText

    private val _pendingLocator = MutableStateFlow<Locator?>(null)
    val pendingLocator: StateFlow<Locator?> = _pendingLocator

    private val _pendingAnnotationType = MutableStateFlow("note")
    val pendingAnnotationType: StateFlow<String> = _pendingAnnotationType

    private val _annotationNoteText = MutableStateFlow("")
    val annotationNoteText: StateFlow<String> = _annotationNoteText

    private val _annotationColorArgb = MutableStateFlow(DEFAULT_ANNOTATION_COLOR)
    val annotationColorArgb: StateFlow<Int> = _annotationColorArgb

    fun startAnnotation(
        type: String = "note",
        prefilledText: String? = null,
        locator: Locator? = null
    ) {
        _pendingAnnotationType.value = type
        _pendingSelectedText.value = prefilledText
        _pendingLocator.value = locator
        _annotationNoteText.value = ""
        _annotationColorArgb.value = DEFAULT_ANNOTATION_COLOR
        _showAnnotationCreator.value = true
    }

    fun setAnnotationNote(text: String) {
        _annotationNoteText.value = text
    }

    fun setAnnotationColor(argb: Int) {
        _annotationColorArgb.value = argb
    }

    fun cancelAnnotation() {
        _showAnnotationCreator.value = false
        _editingAnnotationId.value = null
        _pendingSelectedText.value = null
        _pendingLocator.value = null
        _pendingAnnotationType.value = "note"
        _annotationNoteText.value = ""
        _annotationColorArgb.value = DEFAULT_ANNOTATION_COLOR
    }

    // ── Inline highlight color picker ────────────────────────────────────────

    private val _showHighlightColorPicker = MutableStateFlow(false)
    val showHighlightColorPicker: StateFlow<Boolean> = _showHighlightColorPicker

    fun startHighlightColorPicker(
        text: String?,
        locator: Locator? = null
    ) {
        _pendingSelectedText.value = text
        _pendingLocator.value = locator
        _showHighlightColorPicker.value = true
    }

    fun dismissHighlightColorPicker() {
        _showHighlightColorPicker.value = false
        _pendingSelectedText.value = null
        _pendingLocator.value = null
    }

    // ── Annotation action menu (tap on existing highlight) ───────────────────

    private val _tappedAnnotationId = MutableStateFlow<Long?>(null)
    val tappedAnnotationId: StateFlow<Long?> = _tappedAnnotationId

    fun onAnnotationTapped(id: Long) {
        _tappedAnnotationId.value = id
    }

    fun dismissAnnotationMenu() {
        _tappedAnnotationId.value = null
    }

    // ── Editing an existing annotation (add/change note or color) ────────────

    // Non-null while the CreateAnnotationSheet is editing a saved annotation.
    private val _editingAnnotationId = MutableStateFlow<Long?>(null)
    val editingAnnotationId: StateFlow<Long?> = _editingAnnotationId

    fun startAnnotationEdit(annotation: com.example.liber.data.model.Annotation) {
        _tappedAnnotationId.value = null           // close the action menu
        _editingAnnotationId.value = annotation.id
        _pendingAnnotationType.value = annotation.type.name.lowercase()
        _pendingSelectedText.value = annotation.text
        _annotationNoteText.value = annotation.note ?: ""
        _annotationColorArgb.value = annotation.color
        _showAnnotationCreator.value = true
    }

    // ── Lifecycle ────────────────────────────────────────────────────────────

    override fun onCleared() {
        super.onCleared()
        searchIterator?.close()
    }

    class Factory(
        private val application: Application,
        private val publication: Publication,
        private val userPreferencesRepository: UserPreferencesRepository
    ) : ViewModelProvider.Factory {
        override fun <T : androidx.lifecycle.ViewModel> create(modelClass: Class<T>): T {
            if (modelClass.isAssignableFrom(ReaderViewModel::class.java)) {
                @Suppress("UNCHECKED_CAST")
                return ReaderViewModel(application, publication, userPreferencesRepository) as T
            }
            throw IllegalArgumentException("Unknown ViewModel class")
        }
    }
}
