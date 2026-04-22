package com.example.liber.data.repository

import com.example.liber.data.local.ReadingSessionDao
import com.example.liber.data.model.ReadingSession
import kotlinx.coroutines.flow.Flow

class ReadingInsightsRepository(
    private val readingSessionDao: ReadingSessionDao,
) {
    fun getSessionsSince(threshold: Long): Flow<List<ReadingSession>> {
        return readingSessionDao.getSessionsSince(threshold)
    }

    suspend fun recordSession(
        bookId: String,
        source: String,
        startedAt: Long,
        endedAt: Long,
        durationMillis: Long,
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
