package com.example.liber.data.model

import androidx.core.net.toUri

fun BookEntity.toBook() = Book(
    id = id,
    title = title,
    author = author,
    coverUri = coverPath?.toUri(),
    fileUri = fileUri.toUri(),
    lastOpenedAt = lastOpenedAt,
    wantToRead = wantToRead,
    readingProgress = readingProgress,
    lastLocator = lastLocator,
    contentId = contentId,
    mediaType = mediaType,
    durationMillis = durationMillis,
    narrator = narrator,
    tracksJson = tracksJson,
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
    narrator = narrator,
    tracksJson = tracksJson,
)
