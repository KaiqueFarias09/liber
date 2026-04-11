package com.example.liber.core.util

object InputValidator {
    const val MAX_COLLECTION_NAME_LENGTH = 50
    const val MAX_BOOK_TITLE_LENGTH = 150
    const val MAX_ANNOTATION_LENGTH = 2000

    fun validatedBookTitle(input: String): String {
        return input.take(MAX_BOOK_TITLE_LENGTH)
    }

    fun validatedCollectionName(input: String): String {
        return input.take(MAX_COLLECTION_NAME_LENGTH)
    }

    fun validatedAnnotation(input: String): String {
        return input.take(MAX_ANNOTATION_LENGTH)
    }
}
