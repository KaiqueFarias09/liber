package com.example.liber.data

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "ink_strokes",
    foreignKeys = [
        ForeignKey(
            entity = BookEntity::class,
            parentColumns = ["id"],
            childColumns = ["bookId"],
            onDelete = ForeignKey.CASCADE,
        )
    ],
    indices = [Index("bookId")],
)
data class InkStrokeEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val bookId: String,
    val page: Int,
    /** Stroke width as a fraction of the page width so it scales correctly when the page is zoomed. */
    val strokeWidthFraction: Float,
    val colorArgb: Int,
    val isHighlighter: Boolean,
    /** JSON array of {x, y} pairs with coordinates normalized to [0, 1] relative to the page rect. */
    val pointsJson: String,
    val createdAt: Long = System.currentTimeMillis(),
)
