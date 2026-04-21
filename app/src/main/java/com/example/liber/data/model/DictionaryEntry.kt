package com.example.liber.data.model

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "dictionary_entries",
    foreignKeys = [
        ForeignKey(
            entity = Dictionary::class,
            parentColumns = ["id"],
            childColumns = ["dictionaryId"],
            onDelete = ForeignKey.CASCADE,
        )
    ],
    indices = [
        Index("dictionaryId"),
        Index("normalizedHeadword"),
        Index("languageTag"),
    ]
)
data class DictionaryEntry(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val dictionaryId: String,
    val headword: String,
    val normalizedHeadword: String,
    val lemma: String? = null,
    val languageTag: String,
)
