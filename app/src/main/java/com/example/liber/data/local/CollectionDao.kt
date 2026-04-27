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
        SELECT collections.*, books.*, 
               (SELECT COUNT(*) FROM book_collections WHERE collectionId = collections.id) as totalBooks 
        FROM collections 
        LEFT JOIN book_collections ON collections.id = book_collections.collectionId
        LEFT JOIN books ON book_collections.bookId = books.id
        WHERE books.id IS NULL OR books.id IN (
            SELECT bookId FROM book_collections
            WHERE collectionId = collections.id
            LIMIT 8
        )
        ORDER BY createdAt DESC
    """
    )
    fun getAllCollectionsWithPreviews(): Flow<Map<CollectionWithCount, List<BookPreview>>>

    @Transaction
    @Query("SELECT * FROM collections WHERE id = :id")
    fun getCollectionWithBooks(id: Long): Flow<CollectionWithBooksRelation?>

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
