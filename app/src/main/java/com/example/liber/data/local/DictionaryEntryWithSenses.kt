package com.example.liber.data.local

import androidx.room.Embedded
import androidx.room.Relation
import com.example.liber.data.model.DictionaryEntry
import com.example.liber.data.model.DictionarySense

data class DictionaryEntryWithSenses(
    @Embedded val entry: DictionaryEntry,
    @Relation(
        parentColumn = "id",
        entityColumn = "entryId",
    )
    val senses: List<DictionarySense>,
)
