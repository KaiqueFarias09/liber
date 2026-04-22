package com.example.liber.data.repository

import com.example.liber.core.logging.AppLogger
import com.example.liber.core.logging.BaseRepository
import com.example.liber.data.local.ScanSourceDao
import com.example.liber.data.model.ScanSource
import kotlinx.coroutines.flow.Flow

class ScanSourceRepository(
    private val dao: ScanSourceDao,
    appLogger: AppLogger,
) : BaseRepository("ScanSourceRepository", appLogger) {
    fun getAllSources(): Flow<List<ScanSource>> = observeOperation(
        "getAllSources",
        upstream = dao.getAllSources(),
    )

    suspend fun upsert(source: ScanSource) = executeOperation(
        operationName = "upsert",
        parameters = mapOf(
            "treeUri" to source.treeUri,
            "displayName" to source.displayName,
            "bookCount" to source.bookCount,
        ),
    ) { dao.upsert(source) }

    suspend fun updateScanResult(uri: String, ts: Long, count: Int) {
        executeOperation(
            operationName = "updateScanResult",
            parameters = mapOf("uri" to uri, "timestamp" to ts, "count" to count),
        ) {
            dao.updateScanResult(uri, ts, count)
        }
    }

    suspend fun delete(uri: String) = executeOperation(
        operationName = "delete",
        parameters = mapOf("uri" to uri),
    ) { dao.delete(uri) }
}
