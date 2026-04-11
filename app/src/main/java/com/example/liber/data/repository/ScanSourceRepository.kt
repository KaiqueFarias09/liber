package com.example.liber.data.repository

import com.example.liber.data.local.ScanSourceDao
import com.example.liber.data.model.ScanSourceEntity
import kotlinx.coroutines.flow.Flow

class ScanSourceRepository(private val dao: ScanSourceDao) {
    fun getAllSources(): Flow<List<ScanSourceEntity>> = dao.getAllSources()
    suspend fun upsert(source: ScanSourceEntity) = dao.upsert(source)
    suspend fun updateScanResult(uri: String, ts: Long, count: Int) =
        dao.updateScanResult(uri, ts, count)

    suspend fun delete(uri: String) = dao.delete(uri)
}
