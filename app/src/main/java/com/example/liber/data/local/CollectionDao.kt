package com.example.liber.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import com.example.liber.data.model.BookCollection
import com.example.liber.data.model.BookPreview
import com.example.liber.data.model.Collection
import kotlinx.coroutines.flow.Flow

@Dao
interface CollectionDao {

    @Transaction
    @Query("SELECT * FROM collections ORDER BY createdAt DESC")
    fun getAllCollectionsWithBooks(): Flow<List<CollectionWithBooksRelation>>

    @Transaction
    @Query(
        """
        SELECT 
            c.id AS coll_id, 
            c.name AS coll_name, 
            c.createdAt AS coll_createdAt,
            b.id AS id,
            b.title AS title,
            b.author AS author,
            b.coverUri AS coverUri,
            b.mediaType AS mediaType,
            b.lastOpenedAt AS lastOpenedAt,
            b.wantToRead AS wantToRead,
            b.readingProgress AS readingProgress,
            b.durationMillis AS durationMillis,
            b.addedAt AS addedAt,
            b.finishedAt AS finishedAt,
            (SELECT COUNT(*) FROM book_collections WHERE collectionId = c.id) as totalBooks 
        FROM collections AS c
        LEFT JOIN book_collections ON c.id = book_collections.collectionId
        LEFT JOIN books AS b ON book_collections.bookId = b.id
        WHERE b.id IS NULL OR b.id IN (
            SELECT bookId FROM book_collections
            WHERE collectionId = c.id
            LIMIT 8
        )
        ORDER BY c.createdAt DESC
    """
    )
    fun getAllCollectionsWithPreviews(): Flow<Map<CollectionWithCount, List<BookPreview>>>

    @Transaction
    @Query("SELECT * FROM collections WHERE id = :id")
    fun getCollectionWithBooks(id: Long): Flow<CollectionWithBooksRelation?>

    @Transaction
    @Query(
        """
        SELECT 
            c.id AS coll_id, 
            c.name AS coll_name, 
            c.createdAt AS coll_createdAt,
            b.id AS id, 
            b.title AS title, 
            b.author AS author, 
            b.coverUri AS coverUri, 
            b.mediaType AS mediaType, 
            b.lastOpenedAt AS lastOpenedAt, 
            b.wantToRead AS wantToRead, 
            b.readingProgress AS readingProgress,
            b.durationMillis AS durationMillis,
            b.addedAt AS addedAt,
            b.finishedAt AS finishedAt,
            (SELECT COUNT(*) FROM book_collections WHERE collectionId = c.id) as totalBooks
        FROM collections AS c
        LEFT JOIN book_collections ON c.id = book_collections.collectionId
        LEFT JOIN books AS b ON book_collections.bookId = b.id
        WHERE c.id = :id
    """
    )
    fun getCollectionWithPreviews(id: Long): Flow<Map<CollectionWithCount, List<BookPreview>>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertCollection(collection: Collection): Long

    @Query("UPDATE collections SET name = :name WHERE id = :id")
    suspend fun renameCollection(id: Long, name: String)

    @Query("DELETE FROM collections WHERE id = :id")
    suspend fun deleteCollection(id: Long)

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun addBookToCollection(bookCollection: BookCollection)

    @Query("DELETE FROM book_collections WHERE collectionId = :collectionId AND bookId = :bookId")
    suspend fun removeBookFromCollection(collectionId: Long, bookId: String)

    @Query("SELECT collectionId FROM book_collections WHERE bookId = :bookId")
    fun getCollectionIdsForBook(bookId: String): Flow<List<Long>>
}
