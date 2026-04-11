package com.example.liber.feature.reader

import android.app.Application
import android.content.Context
import androidx.core.content.edit
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import org.readium.r2.shared.ExperimentalReadiumApi
import org.readium.r2.shared.publication.Locator
import org.readium.r2.shared.publication.Publication
import org.readium.r2.shared.publication.services.positions
import org.readium.r2.shared.publication.services.search.SearchIterator
import org.readium.r2.shared.publication.services.search.search

// Default annotation color — yellow at 50 % opacity (visible on both light and dark themes)
private val DEFAULT_ANNOTATION_COLOR = 0x80FFD60A.toInt()

private const val PREFS_NAME = "reader_prefs"
private const val KEY_THEME = "theme_id"
private const val KEY_FONT = "font_size"
private const val KEY_SCROLL = "page_scroll"
private const val KEY_CUSTOMIZE = "customize_layout"
private const val KEY_LINE_SPACING = "line_spacing"
private const val KEY_CHAR_SPACING = "char_spacing"
private const val KEY_WORD_SPACING = "word_spacing"
private const val KEY_MARGINS = "margins"
private const val KEY_COLUMNS = "column_count"
private const val KEY_JUSTIFY = "justify_text"

private const val DEFAULT_LINE_SPACING = 1.4f
private const val DEFAULT_CHAR_SPACING = 0.0f
private const val DEFAULT_WORD_SPACING = 0.0f
private const val DEFAULT_MARGINS = 0.0f

@OptIn(ExperimentalReadiumApi::class)
class ReaderViewModel(
    application: Application,
    val publication: Publication,
) : AndroidViewModel(application) {

    private val prefs = application.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    // ── Reader UI state ──────────────────────────────────────────────────────

    private val _showUI = MutableStateFlow(true)
    val showUI: StateFlow<Boolean> = _showUI

    fun toggleUI() {
        _showUI.value = !_showUI.value
    }

    // ── Theme & font size (persisted) ────────────────────────────────────────

    private val _themeId = MutableStateFlow(prefs.getString(KEY_THEME, "original") ?: "original")
    val themeId: StateFlow<String> = _themeId

    private val _fontSize = MutableStateFlow(prefs.getFloat(KEY_FONT, 1.0f).toDouble())
    val fontSize: StateFlow<Double> = _fontSize

    fun setTheme(id: String) {
        _themeId.value = id
        prefs.edit { putString(KEY_THEME, id) }
    }

    fun adjustFontSize(delta: Double) {
        val next = (_fontSize.value + delta).coerceIn(0.5, 2.0)
        _fontSize.value = next
        prefs.edit { putFloat(KEY_FONT, next.toFloat()) }
    }

    // ── Page flipping (scroll mode) ──────────────────────────────────────────

    private val _pageScroll = MutableStateFlow(prefs.getBoolean(KEY_SCROLL, false))
    val pageScroll: StateFlow<Boolean> = _pageScroll

    fun setPageScroll(scrollMode: Boolean) {
        _pageScroll.value = scrollMode
        prefs.edit { putBoolean(KEY_SCROLL, scrollMode) }
    }

    // ── Layout customization (persisted) ─────────────────────────────────────

    private val _customizeLayout = MutableStateFlow(prefs.getBoolean(KEY_CUSTOMIZE, false))
    val customizeLayout: StateFlow<Boolean> = _customizeLayout

    fun setCustomizeLayout(enabled: Boolean) {
        _customizeLayout.value = enabled
        prefs.edit { putBoolean(KEY_CUSTOMIZE, enabled) }
    }

    private val _lineSpacing =
        MutableStateFlow(prefs.getFloat(KEY_LINE_SPACING, DEFAULT_LINE_SPACING).toDouble())
    val lineSpacing: StateFlow<Double> = _lineSpacing

    fun setLineSpacing(value: Double) {
        _lineSpacing.value = value.coerceIn(0.8, 2.5)
        prefs.edit { putFloat(KEY_LINE_SPACING, _lineSpacing.value.toFloat()) }
    }

    private val _characterSpacing =
        MutableStateFlow(prefs.getFloat(KEY_CHAR_SPACING, DEFAULT_CHAR_SPACING).toDouble())
    val characterSpacing: StateFlow<Double> = _characterSpacing

    fun setCharacterSpacing(value: Double) {
        _characterSpacing.value = value.coerceIn(-10.0, 10.0)
        prefs.edit { putFloat(KEY_CHAR_SPACING, _characterSpacing.value.toFloat()) }
    }

    private val _wordSpacing =
        MutableStateFlow(prefs.getFloat(KEY_WORD_SPACING, DEFAULT_WORD_SPACING).toDouble())
    val wordSpacing: StateFlow<Double> = _wordSpacing

    fun setWordSpacing(value: Double) {
        _wordSpacing.value = value.coerceIn(-20.0, 20.0)
        prefs.edit { putFloat(KEY_WORD_SPACING, _wordSpacing.value.toFloat()) }
    }

    private val _margins =
        MutableStateFlow(prefs.getFloat(KEY_MARGINS, DEFAULT_MARGINS).toDouble())
    val margins: StateFlow<Double> = _margins

    fun setMargins(value: Double) {
        _margins.value = value.coerceIn(-10.0, 10.0)
        prefs.edit { putFloat(KEY_MARGINS, _margins.value.toFloat()) }
    }

    // "auto" | "one" | "two"
    private val _columnCount =
        MutableStateFlow(prefs.getString(KEY_COLUMNS, "auto") ?: "auto")
    val columnCount: StateFlow<String> = _columnCount

    fun setColumnCount(value: String) {
        _columnCount.value = value
        prefs.edit { putString(KEY_COLUMNS, value) }
    }

    private val _justifyText = MutableStateFlow(prefs.getBoolean(KEY_JUSTIFY, false))
    val justifyText: StateFlow<Boolean> = _justifyText

    fun setJustifyText(value: Boolean) {
        _justifyText.value = value
        prefs.edit { putBoolean(KEY_JUSTIFY, value) }
    }

    /** Resets all layout / typography settings to their defaults. */
    fun resetLayoutSettings() {
        setPageScroll(false)
        setCustomizeLayout(false)
        _lineSpacing.value = DEFAULT_LINE_SPACING.toDouble()
        _characterSpacing.value = DEFAULT_CHAR_SPACING.toDouble()
        _wordSpacing.value = DEFAULT_WORD_SPACING.toDouble()
        _margins.value = DEFAULT_MARGINS.toDouble()
        prefs.edit {
            putFloat(KEY_LINE_SPACING, DEFAULT_LINE_SPACING)
            putFloat(KEY_CHAR_SPACING, DEFAULT_CHAR_SPACING)
            putFloat(KEY_WORD_SPACING, DEFAULT_WORD_SPACING)
            putFloat(KEY_MARGINS, DEFAULT_MARGINS)
        }
        setColumnCount("auto")
        setJustifyText(false)
    }

    // ── Search ───────────────────────────────────────────────────────────────

    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery

    private val _searchResults = MutableStateFlow<List<Locator>>(emptyList())
    val searchResults: StateFlow<List<Locator>> = _searchResults

    private val _isSearching = MutableStateFlow(false)
    val isSearching: StateFlow<Boolean> = _isSearching

    private val _positions = MutableStateFlow<List<Locator>>(emptyList())
    val positions: StateFlow<List<Locator>> = _positions

    init {
        viewModelScope.launch {
            _positions.value = publication.positions()
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

    private val _pendingAnnotationType = MutableStateFlow("note")
    val pendingAnnotationType: StateFlow<String> = _pendingAnnotationType

    private val _annotationNoteText = MutableStateFlow("")
    val annotationNoteText: StateFlow<String> = _annotationNoteText

    private val _annotationColorArgb = MutableStateFlow(DEFAULT_ANNOTATION_COLOR)
    val annotationColorArgb: StateFlow<Int> = _annotationColorArgb

    fun startAnnotation(type: String = "note", prefilledText: String? = null) {
        _pendingAnnotationType.value = type
        _pendingSelectedText.value = prefilledText
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
        _pendingAnnotationType.value = "note"
        _annotationNoteText.value = ""
        _annotationColorArgb.value = DEFAULT_ANNOTATION_COLOR
    }

    // ── Inline highlight color picker ────────────────────────────────────────

    private val _showHighlightColorPicker = MutableStateFlow(false)
    val showHighlightColorPicker: StateFlow<Boolean> = _showHighlightColorPicker

    fun startHighlightColorPicker(text: String?) {
        _pendingSelectedText.value = text
        _showHighlightColorPicker.value = true
    }

    fun dismissHighlightColorPicker() {
        _showHighlightColorPicker.value = false
        _pendingSelectedText.value = null
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

    fun startAnnotationEdit(annotation: com.example.liber.data.model.AnnotationEntity) {
        _tappedAnnotationId.value = null           // close the action menu
        _editingAnnotationId.value = annotation.id
        _pendingAnnotationType.value = annotation.type
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
    ) : ViewModelProvider.Factory {
        override fun <T : androidx.lifecycle.ViewModel> create(modelClass: Class<T>): T {
            if (modelClass.isAssignableFrom(ReaderViewModel::class.java)) {
                @Suppress("UNCHECKED_CAST")
                return ReaderViewModel(application, publication) as T
            }
            throw IllegalArgumentException("Unknown ViewModel class")
        }
    }
}
