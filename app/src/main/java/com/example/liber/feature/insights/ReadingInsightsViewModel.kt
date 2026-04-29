package com.example.liber.feature.insights

import androidx.lifecycle.viewModelScope
import com.example.liber.core.logging.AppLogger
import com.example.liber.core.logging.BaseViewModel
import com.example.liber.core.util.UiState
import com.example.liber.data.model.Book
import com.example.liber.data.model.ReadingSession
import com.example.liber.data.repository.BookRepository
import com.example.liber.data.repository.CollectionRepository
import com.example.liber.data.repository.DictionaryRepository
import com.example.liber.data.repository.ReadingInsightsRepository
import com.example.liber.data.repository.UserPreferencesRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import java.time.DayOfWeek
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.temporal.TemporalAdjusters
import javax.inject.Inject
import kotlin.math.roundToInt

data class ReadingInsightsUiModel(
    val weeklyDurationMillis: Long,
    val todayDurationMillis: Long,
    val dailyGoalMinutes: Int,
    val streakDays: Int,
    val bestStreakDays: Int,
    val heatmapCells: List<HeatmapCellUiModel>,
    val averageSessionMinutes: Int,
    val profileTitle: String,
    val profileSubtitle: String,
    val vocabularyCount: Int,
    val obsession: String?,
    val readingNow: List<Book>,
    val finishedThisYear: List<Book>,
    val finishedThisYearCount: Int,
)

data class HeatmapCellUiModel(
    val date: LocalDate,
    val durationMillis: Long,
    val intensity: Int,
)

@HiltViewModel
class ReadingInsightsViewModel @Inject constructor(
    bookRepository: BookRepository,
    readingInsightsRepository: ReadingInsightsRepository,
    userPreferencesRepository: UserPreferencesRepository,
    dictionaryRepository: DictionaryRepository,
    collectionRepository: CollectionRepository,
    appLogger: AppLogger,
) : BaseViewModel("ReadingInsightsViewModel", appLogger) {

    private val zoneId = ZoneId.systemDefault()
    private val sessionLookbackThreshold = System.currentTimeMillis() - SESSION_LOOKBACK_MILLIS
    private val weekThreshold = System.currentTimeMillis() - (7 * 24 * 60 * 60 * 1000L)

    val insightsState: StateFlow<UiState<ReadingInsightsUiModel>> = combine(
        bookRepository.getAllBooks(),
        readingInsightsRepository.getSessionsSince(sessionLookbackThreshold),
        userPreferencesRepository.readingGoalMinutes,
        dictionaryRepository.getLookupCountSince(weekThreshold),
        collectionRepository.getAllCollectionsWithBooks(),
    ) { books, sessions, dailyGoalMinutes, vocabularyCount, collections ->
        buildUiModel(
            books = books,
            sessions = sessions,
            dailyGoalMinutes = dailyGoalMinutes,
            vocabularyCount = vocabularyCount,
            collections = collections,
        )
    }
        .map<ReadingInsightsUiModel, UiState<ReadingInsightsUiModel>> { UiState.Success(it) }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = UiState.Loading,
        )

    private fun buildUiModel(
        books: List<Book>,
        sessions: List<ReadingSession>,
        dailyGoalMinutes: Int,
        vocabularyCount: Int,
        collections: List<com.example.liber.data.local.CollectionWithBooksRelation>,
    ): ReadingInsightsUiModel {
        val today = LocalDate.now(zoneId)
        val weekStart = today.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY))
        val yearStart = today.withDayOfYear(1)

        val durationsByDay = sessions
            .groupBy { session -> session.startedAt.toLocalDate() }
            .mapValues { (_, daySessions) -> daySessions.sumOf { it.durationMillis } }

        val weeklyDurationMillis = durationsByDay
            .filterKeys { it >= weekStart }
            .values
            .sum()

        val todayDurationMillis = durationsByDay[today] ?: 0L

        val heatmapCells = (27L downTo 0L).map { daysAgo ->
            val date = today.minusDays(daysAgo)
            val duration = durationsByDay[date] ?: 0L
            HeatmapCellUiModel(
                date = date,
                durationMillis = duration,
                intensity = duration.toHeatmapIntensity(),
            )
        }

        val activeDays = durationsByDay
            .filterValues { it > 0L }
            .keys

        val streakDays = generateSequence(today) { it.minusDays(1) }
            .takeWhile { it in activeDays }
            .count()

        val bestStreakDays = activeDays.bestStreak()

        val recentSessions = sessions.filter {
            val day = it.startedAt.toLocalDate()
            day >= today.minusDays(29)
        }

        val averageSessionMinutes = if (recentSessions.isEmpty()) {
            0
        } else {
            (recentSessions.map { it.durationMillis }.average() / 60_000.0).roundToInt()
        }

        val (profileTitle, profileSubtitle) = buildReaderProfile(recentSessions)

        val readingNow = books
            .filter { book ->
                book.readingProgress in 1..99
            }
            .sortedByDescending { it.lastOpenedAt ?: 0L }

        val obsession = calculateObsession(readingNow, collections)

        val finishedThisYear = books
            .filter { book ->
                book.readingProgress >= 100 &&
                    book.lastOpenedAt?.toLocalDate()?.let { it >= yearStart } == true
            }
            .sortedByDescending { it.lastOpenedAt ?: 0L }

        return ReadingInsightsUiModel(
            weeklyDurationMillis = weeklyDurationMillis,
            todayDurationMillis = todayDurationMillis,
            dailyGoalMinutes = dailyGoalMinutes,
            streakDays = streakDays,
            bestStreakDays = bestStreakDays,
            heatmapCells = heatmapCells,
            averageSessionMinutes = averageSessionMinutes,
            profileTitle = profileTitle,
            profileSubtitle = profileSubtitle,
            vocabularyCount = vocabularyCount,
            obsession = obsession,
            readingNow = readingNow,
            finishedThisYear = finishedThisYear,
            finishedThisYearCount = finishedThisYear.size,
        )
    }

    private fun calculateObsession(
        readingNow: List<Book>,
        collections: List<com.example.liber.data.local.CollectionWithBooksRelation>,
    ): String? {
        if (readingNow.isEmpty()) return null

        val recentBookIds = readingNow.take(5).map { it.id }.toSet()
        val relevantCollections = collections.filter { relation ->
            relation.books.any { it.id in recentBookIds }
        }

        return relevantCollections
            .maxByOrNull { relation ->
                relation.books.count { it.id in recentBookIds }
            }?.collection?.name
    }

    private fun buildReaderProfile(sessions: List<ReadingSession>): Pair<String, String> {
        if (sessions.isEmpty()) {
            return "Finding Your Rhythm" to "Spend a few sessions with Liber to unlock your reading profile."
        }

        val bucketDurations = sessions.groupBy { session ->
            when (session.startedAt.localHour()) {
                in 5..11 -> ReaderBucket.MORNING
                in 12..17 -> ReaderBucket.AFTERNOON
                else -> ReaderBucket.NIGHT
            }
        }.mapValues { (_, value) -> value.sumOf { it.durationMillis } }

        return when (bucketDurations.maxByOrNull { it.value }?.key ?: ReaderBucket.NIGHT) {
            ReaderBucket.MORNING -> "Morning Reader" to "You do your best page turning before the day fully starts."
            ReaderBucket.AFTERNOON -> "Afternoon Reader" to "Your reading rhythm peaks once the day settles into motion."
            ReaderBucket.NIGHT -> "Night Reader" to "Your longest sessions happen after hours, when everything else quiets down."
        }
    }

    private fun Long.toLocalDate(): LocalDate {
        return Instant.ofEpochMilli(this).atZone(zoneId).toLocalDate()
    }

    private fun Long.localHour(): Int {
        return Instant.ofEpochMilli(this).atZone(zoneId).hour
    }

    private fun Long.toHeatmapIntensity(): Int {
        val minutes = this / 60_000L
        return when {
            minutes <= 0 -> 0
            minutes < 10 -> 1
            minutes < 20 -> 2
            minutes < 40 -> 3
            else -> 4
        }
    }

    private fun Set<LocalDate>.bestStreak(): Int {
        if (isEmpty()) return 0

        val sortedDates = toList().sorted()
        var best = 1
        var current = 1

        for (index in 1 until sortedDates.size) {
            current = if (sortedDates[index - 1].plusDays(1) == sortedDates[index]) {
                current + 1
            } else {
                1
            }
            if (current > best) best = current
        }

        return best
    }

    private enum class ReaderBucket {
        MORNING,
        AFTERNOON,
        NIGHT,
    }

    private companion object {
        const val SESSION_LOOKBACK_MILLIS = 400L * 24L * 60L * 60L * 1000L
    }
}
