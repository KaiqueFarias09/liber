package com.example.liber.data

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class BookRepository(private val bookDao: BookDao) {

    fun getAllBooks(): Flow<List<Book>> =
        bookDao.getAllBooks().map { it.map(BookEntity::toBook) }

    suspend fun getAllBooksList(): List<Book> =
        bookDao.getAllBooksList().map(BookEntity::toBook)

    fun getContinueReadingBooks(threshold: Long): Flow<List<Book>> =
        bookDao.getContinueReadingBooks(threshold).map { it.map(BookEntity::toBook) }

    fun getPreviousBooks(threshold: Long): Flow<List<Book>> =
        bookDao.getPreviousBooks(threshold).map { it.map(BookEntity::toBook) }

    fun getWantToReadBooks(): Flow<List<Book>> =
        bookDao.getWantToReadBooks().map { it.map(BookEntity::toBook) }

    suspend fun getBookByFileUri(fileUri: String): BookEntity? =
        bookDao.getBookByFileUri(fileUri)

    suspend fun getBookByContentId(contentId: String): BookEntity? =
        bookDao.getBookByContentId(contentId)

    suspend fun insertBook(book: BookEntity) =
        bookDao.insertBook(book)

    suspend fun updateMetadata(id: String, title: String, author: String?, narrator: String?) =
        bookDao.updateMetadata(id, title, author, narrator)

    suspend fun updateFullMetadata(
        id: String,
        title: String,
        author: String?,
        coverPath: String?,
        narrator: String?
    ) =
        bookDao.updateFullMetadata(id, title, author, coverPath, narrator)

    suspend fun deleteBook(bookId: String) =
        bookDao.deleteBook(bookId)

    suspend fun updateLastOpenedAt(id: String, timestamp: Long) =
        bookDao.updateLastOpenedAt(id, timestamp)

    suspend fun updateLastOpenedAtQuietly(id: String, timestamp: Long) {
        bookDao.updateLastOpenedAt(id, timestamp)
    }

    suspend fun updateWantToRead(id: String, wantToRead: Boolean) =
        bookDao.updateWantToRead(id, wantToRead)

    suspend fun updateLastLocator(id: String, locator: String?, progress: Int) =
        bookDao.updateLastLocator(id, locator, progress)

    suspend fun updateLastLocatorQuietly(id: String, locator: String?, progress: Int) {
        bookDao.updateLastLocator(id, locator, progress)
    }

    suspend fun updateDuration(id: String, duration: Long) {
        bookDao.updateDuration(id, duration)
    }

    suspend fun updateCoverPath(id: String, coverPath: String?) =
        bookDao.updateCoverPath(id, coverPath)

    suspend fun updateTracks(id: String, tracksJson: String?) =
        bookDao.updateTracks(id, tracksJson)

    suspend fun insertAnnotation(annotation: AnnotationEntity) =
        bookDao.insertAnnotation(annotation)

    fun getAnnotationsForBook(bookId: String): Flow<List<AnnotationEntity>> =
        bookDao.getAnnotationsForBook(bookId)

    suspend fun deleteAnnotation(annotationId: Long) =
        bookDao.deleteAnnotation(annotationId)

    suspend fun insertBookmark(bookmark: BookmarkEntity) =
        bookDao.insertBookmark(bookmark)

    fun getBookmarksForBook(bookId: String): Flow<List<BookmarkEntity>> =
        bookDao.getBookmarksForBook(bookId)

    suspend fun deleteBookmark(bookmarkId: Long) =
        bookDao.deleteBookmark(bookmarkId)
}
