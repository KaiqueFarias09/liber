package com.example.liber.core.util

object InputValidator {
    const val MAX_COLLECTION_NAME_LENGTH = 50
    const val MAX_BOOK_TITLE_LENGTH = 150
    const val MAX_ANNOTATION_LENGTH = 2000

    fun validatedCollectionName(name: String): String {
        return if (name.length > MAX_COLLECTION_NAME_LENGTH) name.take(MAX_COLLECTION_NAME_LENGTH) else name
    }

    fun validatedBookTitle(title: String): String {
        return if (title.length > MAX_BOOK_TITLE_LENGTH) title.take(MAX_BOOK_TITLE_LENGTH) else title
    }

    fun validatedAnnotation(note: String): String {
        return if (note.length > MAX_ANNOTATION_LENGTH) note.take(MAX_ANNOTATION_LENGTH) else note
    }
}
