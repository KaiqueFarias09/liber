package com.example.liber.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import kotlinx.coroutines.flow.Flow

@Dao
interface CollectionDao {

    @Transaction
    @Query("SELECT * FROM collections ORDER BY createdAt DESC")
    fun getAllCollectionsWithBooks(): Flow<List<CollectionWithBooksRelation>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertCollection(collection: CollectionEntity): Long

    @Query("UPDATE collections SET name = :name WHERE id = :id")
    suspend fun renameCollection(id: Long, name: String)

    @Query("DELETE FROM collections WHERE id = :id")
    suspend fun deleteCollection(id: Long)

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun addBookToCollection(bookCollection: BookCollectionEntity)

    @Query("DELETE FROM book_collections WHERE collectionId = :collectionId AND bookId = :bookId")
    suspend fun removeBookFromCollection(collectionId: Long, bookId: String)

    @Query("SELECT collectionId FROM book_collections WHERE bookId = :bookId")
    fun getCollectionIdsForBook(bookId: String): Flow<List<Long>>
}
