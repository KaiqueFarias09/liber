package com.example.liber.data.model

import android.net.Uri
import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "books")
data class Book(
    @PrimaryKey val id: String,
    val title: String,
    val author: String?,
    val coverUri: Uri?,
    val fileUri: Uri,
    val lastOpenedAt: Long? = null,
    val wantToRead: Boolean = false,
    val readingProgress: Int = 0,
    val lastLocator: String? = null,
    val contentId: String? = null,
    val mediaType: String? = null,
    val durationMillis: Long? = null,
    val narrator: String? = null,
    val tracksJson: String? = null,
    val language: String? = null,
    val addedAt: Long = System.currentTimeMillis(),
    val finishedAt: Long? = null,
) {
    val isAudiobook: Boolean
        get() = mediaType == "audio/mpeg" || mediaType == "audiobook"
}
