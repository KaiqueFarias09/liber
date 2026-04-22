package com.example.liber.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.example.liber.data.model.ReadingSession
import kotlinx.coroutines.flow.Flow

@Dao
interface ReadingSessionDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSession(session: ReadingSession)

    @Query("SELECT * FROM reading_sessions WHERE endedAt >= :threshold ORDER BY endedAt DESC")
    fun getSessionsSince(threshold: Long): Flow<List<ReadingSession>>
}
