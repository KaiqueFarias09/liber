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
    val lastLocator: String? = null
)
