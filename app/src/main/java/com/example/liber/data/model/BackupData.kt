package com.example.liber.data.model

import kotlinx.serialization.Serializable

@Serializable
data class BackupData(
    val version: Int = 1,
    val exportedAt: Long = System.currentTimeMillis(),
    val books: List<Book> = emptyList(),
    val annotations: List<Annotation> = emptyList(),
    val bookmarks: List<Bookmark> = emptyList(),
    val collections: List<Collection> = emptyList(),
    val bookCollections: List<BookCollection> = emptyList(),
)
