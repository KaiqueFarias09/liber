package com.example.liber.data.repository

import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ReadingSessionTracker @Inject constructor(
    private val readingInsightsRepository: ReadingInsightsRepository,
) {
    private data class ActiveSession(
        val bookId: String,
        val source: String,
        val startedAt: Long,
    )

    private val mutex = Mutex()
    private val activeSessions = mutableMapOf<String, ActiveSession>()

    suspend fun start(channel: String, bookId: String, source: String) {
        val now = System.currentTimeMillis()
        mutex.withLock {
            val active = activeSessions[channel]
            if (active?.bookId == bookId && active.source == source) return
            stopLocked(channel, now)
            activeSessions[channel] = ActiveSession(
                bookId = bookId,
                source = source,
                startedAt = now,
            )
        }
    }

    suspend fun stop(channel: String) {
        mutex.withLock {
            stopLocked(channel, System.currentTimeMillis())
        }
    }

    private suspend fun stopLocked(channel: String, endedAt: Long) {
        val active = activeSessions.remove(channel) ?: return
        val durationMillis = (endedAt - active.startedAt).coerceAtLeast(0L)
        if (durationMillis < MINIMUM_SESSION_MILLIS) return

        readingInsightsRepository.recordSession(
            bookId = active.bookId,
            source = active.source,
            startedAt = active.startedAt,
            endedAt = endedAt,
            durationMillis = durationMillis,
        )
    }

    private companion object {
        const val MINIMUM_SESSION_MILLIS = 60_000L
    }
}
