package com.example.liber.feature.reader

/**
 * Fired when the user taps "Highlight" or "Add Note" from the text-selection
 * menu inside the reader. Uses CREngine xpointer strings as position markers.
 */
sealed class AnnotationRequest(
    open val selectedText: String?,
    open val xpointer: String? = null
) {
    data class Note(
        override val selectedText: String?,
        override val xpointer: String? = null
    ) : AnnotationRequest(selectedText, xpointer)

    data class Highlight(
        override val selectedText: String?,
        override val xpointer: String? = null
    ) : AnnotationRequest(selectedText, xpointer)
}
