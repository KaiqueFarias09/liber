package com.example.liber.data

import androidx.room.Embedded
import androidx.room.Junction
import androidx.room.Relation

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
