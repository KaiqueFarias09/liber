package com.example.liber.ui.reader

import android.app.Application
import android.content.Context
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import org.readium.r2.shared.ExperimentalReadiumApi
import org.readium.r2.shared.publication.Locator
import org.readium.r2.shared.publication.Publication
import org.readium.r2.shared.publication.services.search.SearchIterator
import org.readium.r2.shared.publication.services.search.search

// Default annotation color — pastel yellow (matches PastelYellow100)
private const val DEFAULT_ANNOTATION_COLOR = 0xFFFFF8DC.toInt()

private const val PREFS_NAME  = "reader_prefs"
private const val KEY_THEME   = "theme_id"
private const val KEY_FONT    = "font_size"

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
        prefs.edit().putString(KEY_THEME, id).apply()
    }

    fun adjustFontSize(delta: Double) {
        val next = (_fontSize.value + delta).coerceIn(0.5, 2.0)
        _fontSize.value = next
        prefs.edit().putFloat(KEY_FONT, next.toFloat()).apply()
    }

    // ── Search ───────────────────────────────────────────────────────────────

    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery

    private val _searchResults = MutableStateFlow<List<Locator>>(emptyList())
    val searchResults: StateFlow<List<Locator>> = _searchResults

    private val _isSearching = MutableStateFlow(false)
    val isSearching: StateFlow<Boolean> = _isSearching

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

    fun setAnnotationNote(text: String) { _annotationNoteText.value = text }
    fun setAnnotationColor(argb: Int)   { _annotationColorArgb.value = argb }

    fun cancelAnnotation() {
        _showAnnotationCreator.value = false
        _pendingSelectedText.value = null
        _pendingAnnotationType.value = "note"
        _annotationNoteText.value = ""
        _annotationColorArgb.value = DEFAULT_ANNOTATION_COLOR
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
