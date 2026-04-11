package com.example.liber.core.util

fun String.limitTo(maxLength: Int): String {
    return if (this.length > maxLength) this.take(maxLength) else this
}

fun String.isValidCollectionName(): Boolean {
    return this.isNotBlank() && this.length <= InputValidator.MAX_COLLECTION_NAME_LENGTH
}

fun String.validatedCollectionName(): String = limitTo(InputValidator.MAX_COLLECTION_NAME_LENGTH)

fun String.validatedBookTitle(): String = limitTo(InputValidator.MAX_BOOK_TITLE_LENGTH)

fun String.validatedAnnotation(): String = limitTo(InputValidator.MAX_ANNOTATION_LENGTH)

fun String.sanitizeForFileName(): String {
    return if (this.isBlank()) "file"
    else this.replace(Regex("[^a-zA-Z0-9._]"), "_")
}
