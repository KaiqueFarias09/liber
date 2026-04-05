package com.example.liber.data

import android.net.Uri
import androidx.core.net.toUri
import java.io.File

fun BookEntity.toBook() = Book(
    id = id,
    title = title,
    author = author,
    coverUri = coverPath?.let { Uri.fromFile(File(it)) },
    fileUri = fileUri.toUri(),
    lastOpenedAt = lastOpenedAt,
    wantToRead = wantToRead,
    readingProgress = readingProgress,
    lastLocator = lastLocator,
)
