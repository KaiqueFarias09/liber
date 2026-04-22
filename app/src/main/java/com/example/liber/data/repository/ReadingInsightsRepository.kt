package com.example.liber.data.repository

import com.example.liber.core.logging.AppLogger
import com.example.liber.core.logging.BaseRepository
import com.example.liber.data.local.ReadingSessionDao
import com.example.liber.data.model.ReadingSession
import kotlinx.coroutines.flow.Flow

class ReadingInsightsRepository(
    private val readingSessionDao: ReadingSessionDao,
    appLogger: AppLogger,
) : BaseRepository("ReadingInsightsRepository", appLogger) {
    fun getSessionsSince(threshold: Long): Flow<List<ReadingSession>> = observeOperation(
        operationName = "getSessionsSince",
        parameters = mapOf("threshold" to threshold),
        upstream = readingSessionDao.getSessionsSince(threshold),
    )

    suspend fun recordSession(
        bookId: String,
        source: String,
        startedAt: Long,
        endedAt: Long,
        durationMillis: Long,
    ) = executeOperation(
        operationName = "recordSession",
        parameters = mapOf(
            "bookId" to bookId,
            "source" to source,
            "startedAt" to startedAt,
            "endedAt" to endedAt,
            "durationMillis" to durationMillis,
        ),
    ) {
        readingSessionDao.insertSession(
            ReadingSession(
                bookId = bookId,
                source = source,
                startedAt = startedAt,
                endedAt = endedAt,
                durationMillis = durationMillis,
            )
        )
    }
}
