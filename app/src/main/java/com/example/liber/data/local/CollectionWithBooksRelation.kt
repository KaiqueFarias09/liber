package com.example.liber.data.local

import androidx.room.Embedded
import androidx.room.Junction
import androidx.room.Relation
import com.example.liber.data.model.Book
import com.example.liber.data.model.BookCollection
import com.example.liber.data.model.Collection

data class CollectionWithBooksRelation(
    @Embedded val collection: Collection,
    @Relation(
        parentColumn = "id",
        entityColumn = "id",
        associateBy = Junction(
            value = BookCollection::class,
            parentColumn = "collectionId",
            entityColumn = "bookId",
        ),
    )
    val books: List<Book>,
)
