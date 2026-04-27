package com.example.liber.data.local

import androidx.room.Embedded
import com.example.liber.data.model.Collection

data class CollectionWithCount(
    @Embedded val collection: Collection,
    val totalBooks: Int,
)
