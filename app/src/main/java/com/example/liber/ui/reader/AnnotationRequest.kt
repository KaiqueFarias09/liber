package com.example.liber.ui.reader

/** Posted by [MainActivity] when the user taps "Add Note" or "Highlight" from the text-selection menu. */
sealed class AnnotationRequest(open val selectedText: String?) {
    data class Note(override val selectedText: String?) : AnnotationRequest(selectedText)
    data class Highlight(override val selectedText: String?) : AnnotationRequest(selectedText)
}
