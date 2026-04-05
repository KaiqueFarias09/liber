package com.example.liber.ui.reader

import androidx.lifecycle.ViewModel
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

@OptIn(ExperimentalReadiumApi::class)
class ReaderViewModel(val publication: Publication) : ViewModel() {

    // ── Reader UI state ──────────────────────────────────────────────────────

    private val _showUI = MutableStateFlow(true)
    val showUI: StateFlow<Boolean> = _showUI

    fun toggleUI() {
        _showUI.value = !_showUI.value
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
                .onFailure {
                    // Handle error
                }
        }
    }

    // ── Annotation creation state ────────────────────────────────────────────

    private val _showAnnotationCreator = MutableStateFlow(false)
    val showAnnotationCreator: StateFlow<Boolean> = _showAnnotationCreator

    /** Text selected in the EPUB before "Highlight" or "Add Note" was tapped. */
    private val _pendingSelectedText = MutableStateFlow<String?>(null)
    val pendingSelectedText: StateFlow<String?> = _pendingSelectedText

    /** "note" or "highlight" — controls the sheet title and save behavior. */
    private val _pendingAnnotationType = MutableStateFlow("note")
    val pendingAnnotationType: StateFlow<String> = _pendingAnnotationType

    private val _annotationNoteText = MutableStateFlow("")
    val annotationNoteText: StateFlow<String> = _annotationNoteText

    private val _annotationColorArgb = MutableStateFlow(DEFAULT_ANNOTATION_COLOR)
    val annotationColorArgb: StateFlow<Int> = _annotationColorArgb

    /**
     * Opens the annotation creator sheet.
     *
     * @param type "note" or "highlight"
     * @param prefilledText Text already selected in the EPUB (from the system text-selection menu).
     */
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

    class Factory(private val publication: Publication) : ViewModelProvider.Factory {
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            if (modelClass.isAssignableFrom(ReaderViewModel::class.java)) {
                @Suppress("UNCHECKED_CAST")
                return ReaderViewModel(publication) as T
            }
            throw IllegalArgumentException("Unknown ViewModel class")
        }
    }
}
