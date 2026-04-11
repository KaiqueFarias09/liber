package com.example.liber.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.example.liber.data.model.ScanSourceEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface ScanSourceDao {
    @Query("SELECT * FROM scan_sources ORDER BY addedAt DESC")
    fun getAllSources(): Flow<List<ScanSourceEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(source: ScanSourceEntity)

    @Query("UPDATE scan_sources SET lastScannedAt = :ts, bookCount = :count WHERE treeUri = :uri")
    suspend fun updateScanResult(uri: String, ts: Long, count: Int)

    @Query("DELETE FROM scan_sources WHERE treeUri = :uri")
    suspend fun delete(uri: String)
}
