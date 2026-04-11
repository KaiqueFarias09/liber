package com.example.liber.data.local

import androidx.room.Embedded
import androidx.room.Junction
import androidx.room.Relation
import com.example.liber.data.model.BookCollectionEntity
import com.example.liber.data.model.BookEntity
import com.example.liber.data.model.CollectionEntity

data class CollectionWithBooksRelation(
    @Embedded val collection: CollectionEntity,
    @Relation(
        parentColumn = "id",
        entityColumn = "id",
        associateBy = Junction(
            value = BookCollectionEntity::class,
            parentColumn = "collectionId",
            entityColumn = "bookId",
        ),
    )
    val books: List<BookEntity>,
)
