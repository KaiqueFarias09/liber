package com.example.liber.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query

@Dao
interface InkStrokeDao {

    @Query("SELECT * FROM ink_strokes WHERE bookId = :bookId ORDER BY createdAt ASC")
    suspend fun getForBook(bookId: String): List<InkStrokeEntity>

    @Insert
    suspend fun insert(stroke: InkStrokeEntity): Long

    @Query("DELETE FROM ink_strokes WHERE bookId = :bookId")
    suspend fun deleteForBook(bookId: String)
}
