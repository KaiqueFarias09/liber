package com.example.liber.data.model

data class Annotation(
    val id: Long = 0,
    val bookId: String,
    val type: AnnotationType,
    val color: Int,
    val locator: String,
    val text: String?,
    val note: String? = null,
    val createdAt: Long = System.currentTimeMillis()
)

enum class AnnotationType {
    HIGHLIGHT, NOTE
}

fun AnnotationEntity.toDomain() = Annotation(
    id = id,
    bookId = bookId,
    type = when (type.lowercase()) {
        "note" -> AnnotationType.NOTE
        else -> AnnotationType.HIGHLIGHT
    },
    color = color,
    locator = locator,
    text = text,
    note = note,
    createdAt = createdAt
)

fun Annotation.toEntity() = AnnotationEntity(
    id = id,
    bookId = bookId,
    type = type.name.lowercase(),
    color = color,
    locator = locator,
    text = text,
    note = note,
    createdAt = createdAt
)
