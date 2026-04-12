package com.example.liber.data.repository

import com.example.liber.data.local.CollectionDao
import com.example.liber.data.local.CollectionWithBooksRelation
import com.example.liber.data.model.BookCollection
import com.example.liber.data.model.Collection
import kotlinx.coroutines.flow.Flow

class CollectionRepository(private val collectionDao: CollectionDao) {

    fun getAllCollectionsWithBooks(): Flow<List<CollectionWithBooksRelation>> {
        return collectionDao.getAllCollectionsWithBooks()
    }

    suspend fun insertCollection(collection: Collection): Long {
        return collectionDao.insertCollection(collection)
    }

    suspend fun renameCollection(id: Long, name: String) {
        collectionDao.renameCollection(id, name)
    }

    suspend fun deleteCollection(id: Long) {
        collectionDao.deleteCollection(id)
    }

    suspend fun addBookToCollection(bookCollection: BookCollection) {
        collectionDao.addBookToCollection(bookCollection)
    }

    suspend fun removeBookFromCollection(collectionId: Long, bookId: String) {
        collectionDao.removeBookFromCollection(collectionId, bookId)
    }
}
