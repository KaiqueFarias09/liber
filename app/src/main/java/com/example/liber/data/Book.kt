package com.example.liber.data

import android.net.Uri

data class Book(
    val id: String,
    val title: String,
    val author: String?,
    val coverUri: Uri?,
    val fileUri: Uri,
    val lastOpenedAt: Long? = null,
    val wantToRead: Boolean = false,
    val readingProgress: Int = 0
)
