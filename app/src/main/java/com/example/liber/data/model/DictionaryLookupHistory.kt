package com.example.liber.data.model

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "dictionary_lookup_history",
    foreignKeys = [
        ForeignKey(
            entity = DictionaryEntry::class,
            parentColumns = ["id"],
            childColumns = ["entryId"],
            onDelete = ForeignKey.SET_NULL,
        ),
        ForeignKey(
            entity = Dictionary::class,
            parentColumns = ["id"],
            childColumns = ["dictionaryId"],
            onDelete = ForeignKey.SET_NULL,
        )
    ],
    indices = [
        Index("entryId"),
        Index("dictionaryId"),
        Index("lookedUpAt"),
    ]
)
data class DictionaryLookupHistory(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val query: String,
    val entryId: Long? = null,
    val dictionaryId: String? = null,
    val sourceBookId: String? = null,
    val lookedUpAt: Long = System.currentTimeMillis(),
)
