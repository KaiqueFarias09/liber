package com.example.liber.data.repository

import com.example.liber.core.logging.AppLogger
import com.example.liber.core.logging.BaseRepository
import com.example.liber.data.local.BookDao
import com.example.liber.data.model.Annotation
import com.example.liber.data.model.Book
import com.example.liber.data.model.BookPreview
import com.example.liber.data.model.Bookmark
import kotlinx.coroutines.flow.Flow

class BookRepository(
    private val bookDao: BookDao,
    appLogger: AppLogger,
) : BaseRepository("BookRepository", appLogger) {

    fun getAllBooks(): Flow<List<Book>> =
        observeOperation("getAllBooks", upstream = bookDao.getAllBooks())

    fun getAllBookPreviews(): Flow<List<BookPreview>> = observeOperation(
        "getAllBookPreviews",
        upstream = bookDao.getAllBookPreviews(),
    )

    suspend fun getBookById(id: String): Book? = executeOperation(
        operationName = "getBookById",
        parameters = mapOf("id" to id),
    ) { bookDao.getBookById(id) }

    fun getContinueReadingBookPreviews(threshold: Long): Flow<List<BookPreview>> = observeOperation(
        operationName = "getContinueReadingBookPreviews",
        parameters = mapOf("threshold" to threshold),
        upstream = bookDao.getContinueReadingBookPreviews(threshold),
    )

    fun getPreviousBookPreviews(threshold: Long): Flow<List<BookPreview>> = observeOperation(
        operationName = "getPreviousBookPreviews",
        parameters = mapOf("threshold" to threshold),
        upstream = bookDao.getPreviousBookPreviews(threshold),
    )

    fun getWantToReadBookPreviews(): Flow<List<BookPreview>> = observeOperation(
        "getWantToReadBookPreviews",
        upstream = bookDao.getWantToReadBookPreviews(),
    )

    suspend fun getBookByFileUri(fileUri: String): Book? = executeOperation(
        operationName = "getBookByFileUri",
        parameters = mapOf("fileUri" to fileUri),
    ) { bookDao.getBookByFileUri(fileUri) }

    suspend fun getBookByContentId(contentId: String): Book? = executeOperation(
        operationName = "getBookByContentId",
        parameters = mapOf("contentId" to contentId),
    ) { bookDao.getBookByContentId(contentId) }

    suspend fun getAllBooksList(): List<Book> = executeOperation("getAllBooksList") {
        bookDao.getAllBooksList()
    }

    fun getAllAnnotations(): Flow<List<Annotation>> = observeOperation(
        "getAllAnnotations",
        upstream = bookDao.getAllAnnotations()
    )

    fun getAllBookmarks(): Flow<List<Bookmark>> = observeOperation(
        "getAllBookmarks",
        upstream = bookDao.getAllBookmarks()
    )

    suspend fun insertBook(book: Book) = executeOperation(
        operationName = "insertBook",
        parameters = mapOf("bookId" to book.id, "mediaType" to book.mediaType),
    ) {
        bookDao.insertBook(book)
    }

    suspend fun updateMetadata(id: String, title: String, author: String?, narrator: String?) =
        executeOperation(
            operationName = "updateMetadata",
            parameters = mapOf(
                "id" to id,
                "title" to title,
                "author" to author,
                "narrator" to narrator
            ),
        ) {
            bookDao.updateMetadata(id, title, author, narrator)
        }

    suspend fun updateFullMetadata(
        id: String,
        title: String,
        author: String?,
        coverPath: String?,
        narrator: String?
    ) = executeOperation(
        operationName = "updateFullMetadata",
        parameters = mapOf(
            "id" to id,
            "title" to title,
            "author" to author,
            "hasCover" to (coverPath != null),
            "narrator" to narrator
        ),
    ) {
        bookDao.updateFullMetadata(id, title, author, coverPath, narrator)
    }

    suspend fun deleteBook(bookId: String) = executeOperation(
        operationName = "deleteBook",
        parameters = mapOf("bookId" to bookId),
    ) {
        bookDao.deleteBook(bookId)
    }

    suspend fun updateLastOpenedAt(id: String, timestamp: Long) = executeOperation(
        operationName = "updateLastOpenedAt",
        parameters = mapOf("id" to id, "timestamp" to timestamp),
    ) {
        bookDao.updateLastOpenedAt(id, timestamp)
    }

    suspend fun updateLastOpenedAtQuietly(id: String, timestamp: Long) = executeOperation(
        operationName = "updateLastOpenedAtQuietly",
        parameters = mapOf("id" to id, "timestamp" to timestamp),
    ) {
        bookDao.updateLastOpenedAt(id, timestamp)
    }

    suspend fun updateWantToRead(id: String, wantToRead: Boolean) = executeOperation(
        operationName = "updateWantToRead",
        parameters = mapOf("id" to id, "wantToRead" to wantToRead),
    ) {
        bookDao.updateWantToRead(id, wantToRead)
    }

    suspend fun updateLastLocator(id: String, locator: String?, progress: Int) = executeOperation(
        operationName = "updateLastLocator",
        parameters = mapOf("id" to id, "hasLocator" to (locator != null), "progress" to progress),
    ) {
        bookDao.updateLastLocator(id, locator, progress)
        if (progress == 100) {
            bookDao.updateFinishedAt(id, System.currentTimeMillis())
        } else {
            bookDao.updateFinishedAt(id, null)
        }
    }

    suspend fun updateLastLocatorQuietly(id: String, locator: String?, progress: Int) =
        executeOperation(
            operationName = "updateLastLocatorQuietly",
            parameters = mapOf(
                "id" to id,
                "hasLocator" to (locator != null),
                "progress" to progress
            ),
        ) {
            bookDao.updateLastLocator(id, locator, progress)
            if (progress == 100) {
                bookDao.updateFinishedAt(id, System.currentTimeMillis())
            }
        }

    suspend fun updateDuration(id: String, duration: Long) = executeOperation(
        operationName = "updateDuration",
        parameters = mapOf("id" to id, "duration" to duration),
    ) {
        bookDao.updateDuration(id, duration)
    }

    suspend fun updateCoverPath(id: String, coverPath: String?) = executeOperation(
        operationName = "updateCoverPath",
        parameters = mapOf("id" to id, "hasCoverPath" to (coverPath != null)),
    ) {
        bookDao.updateCoverPath(id, coverPath)
    }

    suspend fun updateTracks(id: String, tracksJson: String?) = executeOperation(
        operationName = "updateTracks",
        parameters = mapOf("id" to id, "hasTracks" to (tracksJson != null)),
    ) {
        bookDao.updateTracks(id, tracksJson)
    }

    suspend fun insertAnnotation(annotation: Annotation) = executeOperation(
        operationName = "insertAnnotation",
        parameters = mapOf("bookId" to annotation.bookId),
    ) {
        bookDao.insertAnnotation(annotation)
    }

    fun getAnnotationsForBook(bookId: String): Flow<List<Annotation>> = observeOperation(
        operationName = "getAnnotationsForBook",
        parameters = mapOf("bookId" to bookId),
        upstream = bookDao.getAnnotationsForBook(bookId),
    )

    suspend fun deleteAnnotation(annotationId: Long) = executeOperation(
        operationName = "deleteAnnotation",
        parameters = mapOf("annotationId" to annotationId),
    ) {
        bookDao.deleteAnnotation(annotationId)
    }

    suspend fun insertBookmark(bookmark: Bookmark) = executeOperation(
        operationName = "insertBookmark",
        parameters = mapOf("bookId" to bookmark.bookId, "locator" to bookmark.locator),
    ) {
        bookDao.insertBookmark(bookmark)
    }

    fun getBookmarksForBook(bookId: String): Flow<List<Bookmark>> = observeOperation(
        operationName = "getBookmarksForBook",
        parameters = mapOf("bookId" to bookId),
        upstream = bookDao.getBookmarksForBook(bookId),
    )

    suspend fun deleteBookmark(bookmarkId: Long) = executeOperation(
        operationName = "deleteBookmark",
        parameters = mapOf("bookmarkId" to bookmarkId),
    ) {
        bookDao.deleteBookmark(bookmarkId)
    }
}
