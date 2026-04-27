package com.example.liber.data.repository

import com.example.liber.core.logging.AppLogger
import com.example.liber.core.logging.BaseRepository
import com.example.liber.data.local.CollectionDao
import com.example.liber.data.local.CollectionWithBooksRelation
import com.example.liber.data.local.CollectionWithCount
import com.example.liber.data.model.BookCollection
import com.example.liber.data.model.BookPreview
import com.example.liber.data.model.Collection
import kotlinx.coroutines.flow.Flow

class CollectionRepository(
    private val collectionDao: CollectionDao,
    appLogger: AppLogger,
) : BaseRepository("CollectionRepository", appLogger) {

    fun getAllCollectionsWithBooks(): Flow<List<CollectionWithBooksRelation>> = observeOperation(
        "getAllCollectionsWithBooks",
        upstream = collectionDao.getAllCollectionsWithBooks(),
    )

    fun getAllCollectionsWithPreviews(): Flow<Map<CollectionWithCount, List<BookPreview>>> =
        observeOperation(
            "getAllCollectionsWithPreviews",
            upstream = collectionDao.getAllCollectionsWithPreviews(),
        )

    fun getCollectionWithBooks(id: Long): Flow<CollectionWithBooksRelation?> = observeOperation(
        "getCollectionWithBooks",
        parameters = mapOf("id" to id),
        upstream = collectionDao.getCollectionWithBooks(id),
    )

    fun getCollectionWithPreviews(id: Long): Flow<Map<Collection, List<BookPreview>>> =
        observeOperation(
            "getCollectionWithPreviews",
            parameters = mapOf("id" to id),
            upstream = collectionDao.getCollectionWithPreviews(id),
        )

    suspend fun insertCollection(collection: Collection): Long = executeOperation(
        operationName = "insertCollection",
        parameters = mapOf("name" to collection.name),
    ) { collectionDao.insertCollection(collection) }

    suspend fun renameCollection(id: Long, name: String) = executeOperation(
        operationName = "renameCollection",
        parameters = mapOf("id" to id, "name" to name),
    ) {
        collectionDao.renameCollection(id, name)
    }

    suspend fun deleteCollection(id: Long) = executeOperation(
        operationName = "deleteCollection",
        parameters = mapOf("id" to id),
    ) {
        collectionDao.deleteCollection(id)
    }

    suspend fun addBookToCollection(bookCollection: BookCollection) = executeOperation(
        operationName = "addBookToCollection",
        parameters = mapOf(
            "collectionId" to bookCollection.collectionId,
            "bookId" to bookCollection.bookId
        ),
    ) {
        collectionDao.addBookToCollection(bookCollection)
    }

    suspend fun removeBookFromCollection(collectionId: Long, bookId: String) = executeOperation(
        operationName = "removeBookFromCollection",
        parameters = mapOf("collectionId" to collectionId, "bookId" to bookId),
    ) {
        collectionDao.removeBookFromCollection(collectionId, bookId)
    }
}
