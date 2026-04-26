package com.example.liber.data.model

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "word_lemmas",
    indices = [
        Index("inflection"),
        Index("languageTag")
    ]
)
data class WordLemma(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val languageTag: String,
    val inflection: String,
    val lemma: String
)
