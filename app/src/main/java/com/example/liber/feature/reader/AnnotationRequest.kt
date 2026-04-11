package com.example.liber.feature.reader

import org.readium.r2.shared.publication.Locator

/** Posted by [MainActivity] when the user taps "Add Note" or "Highlight" from the text-selection menu. */
sealed class AnnotationRequest(open val selectedText: String?, open val locator: Locator? = null) {
    data class Note(
        override val selectedText: String?,
        override val locator: Locator? = null
    ) : AnnotationRequest(selectedText, locator)

    data class Highlight(
        override val selectedText: String?,
        override val locator: Locator? = null
    ) : AnnotationRequest(selectedText, locator)
}
