package com.example.liber.data.repository

import com.example.liber.data.local.ScanSourceDao
import com.example.liber.data.model.ScanSource
import kotlinx.coroutines.flow.Flow

class ScanSourceRepository(private val dao: ScanSourceDao) {
    fun getAllSources(): Flow<List<ScanSource>> = dao.getAllSources()
    suspend fun upsert(source: ScanSource) = dao.upsert(source)
    suspend fun updateScanResult(uri: String, ts: Long, count: Int) {
        dao.updateScanResult(uri, ts, count)
    }

    suspend fun delete(uri: String) = dao.delete(uri)
}
