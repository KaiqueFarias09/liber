package com.example.liber.data.repository

import com.example.liber.data.local.BookDao
import com.example.liber.data.local.CollectionDao
import com.example.liber.data.model.BackupData
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class BackupRepository @Inject constructor(
    private val bookDao: BookDao,
    private val collectionDao: CollectionDao,
) {
    private val json = Json {
        prettyPrint = true
        ignoreUnknownKeys = true
        encodeDefaults = true
    }

    suspend fun createBackupJson(): String = withContext(Dispatchers.IO) {
        val backupData = BackupData(
            books = bookDao.getAllBooksList(),
            annotations = bookDao.getAllAnnotationsList(),
            bookmarks = bookDao.getAllBookmarksList(),
            collections = collectionDao.getAllCollectionsList(),
            bookCollections = collectionDao.getAllBookCollectionsList(),
        )
        json.encodeToString(backupData)
    }

    suspend fun restoreFromBackupJson(jsonString: String) = withContext(Dispatchers.IO) {
        val backupData = json.decodeFromString<BackupData>(jsonString)
        
        // Restore in order to satisfy foreign keys
        bookDao.insertBooks(backupData.books)
        bookDao.insertAnnotations(backupData.annotations)
        bookDao.insertBookmarks(backupData.bookmarks)
        collectionDao.insertCollections(backupData.collections)
        collectionDao.insertBookCollections(backupData.bookCollections)
    }
}
