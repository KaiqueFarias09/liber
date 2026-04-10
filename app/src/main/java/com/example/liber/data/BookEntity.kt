package com.example.liber.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "books")
data class BookEntity(
    @PrimaryKey val id: String,
    val title: String,
    val author: String?,
    val coverPath: String?,
    val fileUri: String,
    val lastOpenedAt: Long? = null,
    val wantToRead: Boolean = false,
    val readingProgress: Int = 0,
    val lastLocator: String? = null,
    val contentId: String? = null,
    val mediaType: String? = null,
    val durationMillis: Long? = null,
    val narrator: String? = null,
    val tracksJson: String? = null,
)
