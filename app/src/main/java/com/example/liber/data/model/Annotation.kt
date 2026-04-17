package com.example.liber.data.model

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "annotations",
    foreignKeys = [
        ForeignKey(
            entity = Book::class,
            parentColumns = ["id"],
            childColumns = ["bookId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index("bookId")]
)
data class Annotation(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val bookId: String,
    val type: AnnotationType,
    val color: Int,
    val locator: String,
    val endLocator: String = "",
    val text: String?,
    val note: String? = null,
    val createdAt: Long = System.currentTimeMillis()
)

enum class AnnotationType {
    HIGHLIGHT, NOTE
}
