package com.example.liber.data.repository

import com.example.liber.data.local.BookDao
import com.example.liber.data.model.Annotation
import com.example.liber.data.model.Book
import com.example.liber.data.model.Bookmark
import kotlinx.coroutines.flow.Flow

class BookRepository(private val bookDao: BookDao) {

    fun getAllBooks(): Flow<List<Book>> {
        return bookDao.getAllBooks()
    }

    fun getContinueReadingBooks(threshold: Long): Flow<List<Book>> {
        return bookDao.getContinueReadingBooks(threshold)
    }

    fun getPreviousBooks(threshold: Long): Flow<List<Book>> {
        return bookDao.getPreviousBooks(threshold)
    }

    fun getWantToReadBooks(): Flow<List<Book>> {
        return bookDao.getWantToReadBooks()
    }

    suspend fun getBookByFileUri(fileUri: String): Book? {
        return bookDao.getBookByFileUri(fileUri)
    }

    suspend fun getBookByContentId(contentId: String): Book? {
        return bookDao.getBookByContentId(contentId)
    }

    suspend fun insertBook(book: Book) {
        bookDao.insertBook(book)
    }

    suspend fun updateMetadata(id: String, title: String, author: String?, narrator: String?) {
        bookDao.updateMetadata(id, title, author, narrator)
    }

    suspend fun updateFullMetadata(
        id: String,
        title: String,
        author: String?,
        coverPath: String?,
        narrator: String?
    ) {
        bookDao.updateFullMetadata(id, title, author, coverPath, narrator)
    }

    suspend fun deleteBook(bookId: String) {
        bookDao.deleteBook(bookId)
    }

    suspend fun updateLastOpenedAt(id: String, timestamp: Long) {
        bookDao.updateLastOpenedAt(id, timestamp)
    }

    suspend fun updateLastOpenedAtQuietly(id: String, timestamp: Long) {
        bookDao.updateLastOpenedAt(id, timestamp)
    }

    suspend fun updateWantToRead(id: String, wantToRead: Boolean) {
        bookDao.updateWantToRead(id, wantToRead)
    }

    suspend fun updateLastLocator(id: String, locator: String?, progress: Int) {
        bookDao.updateLastLocator(id, locator, progress)
    }

    suspend fun updateLastLocatorQuietly(id: String, locator: String?, progress: Int) {
        bookDao.updateLastLocator(id, locator, progress)
    }

    suspend fun updateDuration(id: String, duration: Long) {
        bookDao.updateDuration(id, duration)
    }

    suspend fun updateCoverPath(id: String, coverPath: String?) {
        bookDao.updateCoverPath(id, coverPath)
    }

    suspend fun updateTracks(id: String, tracksJson: String?) {
        bookDao.updateTracks(id, tracksJson)
    }

    suspend fun insertAnnotation(annotation: Annotation) {
        bookDao.insertAnnotation(annotation)
    }

    fun getAnnotationsForBook(bookId: String): Flow<List<Annotation>> {
        return bookDao.getAnnotationsForBook(bookId)
    }

    suspend fun deleteAnnotation(annotationId: Long) {
        bookDao.deleteAnnotation(annotationId)
    }

    suspend fun insertBookmark(bookmark: Bookmark) {
        bookDao.insertBookmark(bookmark)
    }

    fun getBookmarksForBook(bookId: String): Flow<List<Bookmark>> {
        return bookDao.getBookmarksForBook(bookId)
    }

    suspend fun deleteBookmark(bookmarkId: Long) {
        bookDao.deleteBookmark(bookmarkId)
    }
}
