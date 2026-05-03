package com.example.liber.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.example.liber.data.model.Annotation
import com.example.liber.data.model.Book
import com.example.liber.data.model.BookPreview
import com.example.liber.data.model.Bookmark
import kotlinx.coroutines.flow.Flow

@Dao
interface BookDao {
    @Query("SELECT * FROM books ORDER BY title ASC")
    fun getAllBooks(): Flow<List<Book>>

    @Query("SELECT id, title, author, coverUri, mediaType, lastOpenedAt, wantToRead, readingProgress, durationMillis, addedAt, finishedAt FROM books ORDER BY title ASC")
    fun getAllBookPreviews(): Flow<List<BookPreview>>

    @Query("SELECT * FROM books WHERE id = :id")
    suspend fun getBookById(id: String): Book?

    @Query("SELECT * FROM books ORDER BY title ASC")
    suspend fun getAllBooksList(): List<Book>

    @Query("SELECT * FROM annotations ORDER BY createdAt DESC")
    fun getAllAnnotations(): Flow<List<Annotation>>

    @Query("SELECT * FROM bookmarks ORDER BY createdAt DESC")
    fun getAllBookmarks(): Flow<List<Bookmark>>

    @Query("SELECT * FROM annotations ORDER BY createdAt DESC")
    suspend fun getAllAnnotationsList(): List<Annotation>

    @Query("SELECT * FROM bookmarks ORDER BY createdAt DESC")
    suspend fun getAllBookmarksList(): List<Bookmark>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertBooks(books: List<Book>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAnnotations(annotations: List<Annotation>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertBookmarks(bookmarks: List<Bookmark>)

    @Query("SELECT id, title, author, coverUri, mediaType, lastOpenedAt, wantToRead, readingProgress, durationMillis, addedAt, finishedAt FROM books WHERE wantToRead = 0 AND lastOpenedAt IS NOT NULL AND lastOpenedAt >= :threshold ORDER BY lastOpenedAt DESC")
    fun getContinueReadingBookPreviews(threshold: Long): Flow<List<BookPreview>>

    @Query("SELECT id, title, author, coverUri, mediaType, lastOpenedAt, wantToRead, readingProgress, durationMillis, addedAt, finishedAt FROM books WHERE lastOpenedAt IS NOT NULL AND lastOpenedAt < :threshold ORDER BY lastOpenedAt DESC")
    fun getPreviousBookPreviews(threshold: Long): Flow<List<BookPreview>>

    @Query("SELECT id, title, author, coverUri, mediaType, lastOpenedAt, wantToRead, readingProgress, durationMillis, addedAt, finishedAt FROM books WHERE wantToRead = 1 ORDER BY title ASC")
    fun getWantToReadBookPreviews(): Flow<List<BookPreview>>

    @Query("SELECT * FROM books WHERE fileUri = :fileUri LIMIT 1")
    suspend fun getBookByFileUri(fileUri: String): Book?

    @Query("SELECT * FROM books WHERE contentId = :contentId LIMIT 1")
    suspend fun getBookByContentId(contentId: String): Book?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertBook(book: Book)

    @Query("UPDATE books SET title = :title, author = :author, narrator = :narrator WHERE id = :id")
    suspend fun updateMetadata(id: String, title: String, author: String?, narrator: String?)

    @Query("UPDATE books SET title = :title, author = :author, coverUri = :coverPath, narrator = :narrator WHERE id = :id")
    suspend fun updateFullMetadata(
        id: String,
        title: String,
        author: String?,
        coverPath: String?,
        narrator: String?
    )

    @Query("DELETE FROM books WHERE id = :bookId")
    suspend fun deleteBook(bookId: String)

    @Query("UPDATE books SET lastOpenedAt = :timestamp WHERE id = :id")
    suspend fun updateLastOpenedAt(id: String, timestamp: Long)

    @Query("UPDATE books SET wantToRead = :wantToRead WHERE id = :id")
    suspend fun updateWantToRead(id: String, wantToRead: Boolean)

    @Query("UPDATE books SET lastLocator = :locator, readingProgress = :progress WHERE id = :id")
    suspend fun updateLastLocator(id: String, locator: String?, progress: Int)

    @Query("UPDATE books SET finishedAt = :finishedAt WHERE id = :id")
    suspend fun updateFinishedAt(id: String, finishedAt: Long?)

    @Query("UPDATE books SET durationMillis = :duration WHERE id = :id")
    suspend fun updateDuration(id: String, duration: Long)

    @Query("UPDATE books SET coverUri = :coverPath WHERE id = :id")
    suspend fun updateCoverPath(id: String, coverPath: String?)

    @Query("UPDATE books SET tracksJson = :tracksJson WHERE id = :id")
    suspend fun updateTracks(id: String, tracksJson: String?)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAnnotation(annotation: Annotation)

    @Query("SELECT * FROM annotations WHERE bookId = :bookId ORDER BY createdAt DESC")
    fun getAnnotationsForBook(bookId: String): Flow<List<Annotation>>

    @Query("DELETE FROM annotations WHERE id = :annotationId")
    suspend fun deleteAnnotation(annotationId: Long)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertBookmark(bookmark: Bookmark)

    @Query("SELECT * FROM bookmarks WHERE bookId = :bookId ORDER BY createdAt DESC")
    fun getBookmarksForBook(bookId: String): Flow<List<Bookmark>>

    @Query("DELETE FROM bookmarks WHERE id = :bookmarkId")
    suspend fun deleteBookmark(bookmarkId: Long)
}
