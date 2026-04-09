package com.example.liber.data

import android.net.Uri
import androidx.core.net.toUri

fun BookEntity.toBook() = Book(
    id = id,
    title = title,
    author = author,
    coverUri = coverPath?.let { Uri.parse(it) },
    fileUri = fileUri.toUri(),
    lastOpenedAt = lastOpenedAt,
    wantToRead = wantToRead,
    readingProgress = readingProgress,
    lastLocator = lastLocator,
    contentId = contentId,
    mediaType = mediaType,
    durationMillis = durationMillis,
)

fun Book.toEntity() = BookEntity(
    id = id,
    title = title,
    author = author,
    coverPath = coverUri?.toString(),
    fileUri = fileUri.toString(),
    contentId = contentId,
    mediaType = mediaType,
    durationMillis = durationMillis,
)
