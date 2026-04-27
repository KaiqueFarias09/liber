package com.example.liber.data.model

import android.net.Uri

data class BookPreview(
    val id: String,
    val title: String,
    val author: String?,
    val coverUri: Uri?,
    val mediaType: String?,
    val lastOpenedAt: Long? = null,
    val wantToRead: Boolean = false,
    val readingProgress: Int = 0,
    val durationMillis: Long? = null,
) {
    val isAudiobook: Boolean
        get() = mediaType == "audio/mpeg" || mediaType == "audiobook"
}
