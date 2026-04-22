package com.example.liber.data.model

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "reading_sessions",
    foreignKeys = [
        ForeignKey(
            entity = Book::class,
            parentColumns = ["id"],
            childColumns = ["bookId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index("bookId"), Index("startedAt"), Index("endedAt")]
)
data class ReadingSession(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val bookId: String,
    val source: String,
    val startedAt: Long,
    val endedAt: Long,
    val durationMillis: Long,
)

object ReadingSessionSource {
    const val EPUB = "epub"
    const val AUDIOBOOK = "audiobook"
}
