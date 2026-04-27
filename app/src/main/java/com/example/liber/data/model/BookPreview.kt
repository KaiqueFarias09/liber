package com.example.liber.data.model

import android.net.Uri

data class BookPreview(
    val id: String,
    val title: String,
    val author: String?,
    val coverUri: Uri?,
    val mediaType: String?,
) {
    val isAudiobook: Boolean
        get() = mediaType == "audio/mpeg" || mediaType == "audiobook"
}
