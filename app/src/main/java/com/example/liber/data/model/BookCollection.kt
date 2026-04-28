package com.example.liber.data.model

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import kotlinx.serialization.Serializable

@Entity(
    tableName = "book_collections",
    primaryKeys = ["collectionId", "bookId"],
    foreignKeys = [
        ForeignKey(
            entity = Collection::class,
            parentColumns = ["id"],
            childColumns = ["collectionId"],
            onDelete = ForeignKey.CASCADE,
        ),
        ForeignKey(
            entity = Book::class,
            parentColumns = ["id"],
            childColumns = ["bookId"],
            onDelete = ForeignKey.CASCADE,
        ),
    ],
    indices = [Index("collectionId"), Index("bookId")],
)
@Serializable
data class BookCollection(
    val collectionId: Long,
    val bookId: String,
)
