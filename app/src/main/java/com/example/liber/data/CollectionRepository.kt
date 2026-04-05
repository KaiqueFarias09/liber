package com.example.liber.data

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
