package com.example.liber.data

import android.net.Uri

data class Book(
    val id: String,
    val title: String,
    val author: String?,
    val coverUri: Uri?,
    val fileUri: Uri
)
