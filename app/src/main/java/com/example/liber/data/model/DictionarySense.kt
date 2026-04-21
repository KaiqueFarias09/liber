package com.example.liber.data.model

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "dictionary_senses",
    foreignKeys = [
        ForeignKey(
            entity = DictionaryEntry::class,
            parentColumns = ["id"],
            childColumns = ["entryId"],
            onDelete = ForeignKey.CASCADE,
        )
    ],
    indices = [Index("entryId")],
)
data class DictionarySense(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val entryId: Long,
    val partOfSpeech: String? = null,
    val definition: String,
    val example: String? = null,
)
