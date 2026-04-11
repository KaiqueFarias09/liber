package com.example.liber.data.repository

import com.example.liber.data.local.CollectionDao
import com.example.liber.data.local.CollectionWithBooksRelation
import com.example.liber.data.model.BookCollectionEntity
import com.example.liber.data.model.CollectionEntity
import kotlinx.coroutines.flow.Flow

class CollectionRepository(private val collectionDao: CollectionDao) {

    fun getAllCollectionsWithBooks(): Flow<List<CollectionWithBooksRelation>> =
        collectionDao.getAllCollectionsWithBooks()

    suspend fun insertCollection(collection: CollectionEntity): Long =
        collectionDao.insertCollection(collection)

    suspend fun renameCollection(id: Long, name: String) =
        collectionDao.renameCollection(id, name)

    suspend fun deleteCollection(id: Long) =
        collectionDao.deleteCollection(id)

    suspend fun addBookToCollection(bookCollection: BookCollectionEntity) =
        collectionDao.addBookToCollection(bookCollection)

    suspend fun removeBookFromCollection(collectionId: Long, bookId: String) =
        collectionDao.removeBookFromCollection(collectionId, bookId)

    fun getCollectionIdsForBook(bookId: String): Flow<List<Long>> =
        collectionDao.getCollectionIdsForBook(bookId)
}
