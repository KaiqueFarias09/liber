package com.example.liber.feature.collections

import android.app.Application
import androidx.lifecycle.viewModelScope
import com.example.liber.core.logging.AppLogger
import com.example.liber.core.logging.BaseAndroidViewModel
import com.example.liber.core.util.UiState
import com.example.liber.data.model.BookCollection
import com.example.liber.data.model.BookPreview
import com.example.liber.data.model.Collection
import com.example.liber.data.repository.BookRepository
import com.example.liber.data.repository.CollectionRepository
import com.example.liber.data.repository.UserPreferencesRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import java.util.Calendar
import javax.inject.Inject

data class CollectionUiState(
    val id: Long,
    val name: String,
    val previews: List<BookPreview>,
    val totalBooks: Int,
    val isSmart: Boolean = false,
)

@HiltViewModel
class CollectionsViewModel @Inject constructor(
    application: Application,
    private val collectionRepository: CollectionRepository,
    private val bookRepository: BookRepository,
    private val userPreferencesRepository: UserPreferencesRepository,
    appLogger: AppLogger,
) : BaseAndroidViewModel(application, "CollectionsViewModel", appLogger) {

    private val userCollections = collectionRepository
        .getAllCollectionsWithPreviews()
        .map { map ->
            map.map { (withCount, previews) ->
                CollectionUiState(
                    id = withCount.collection.id,
                    name = withCount.collection.name,
                    previews = previews,
                    totalBooks = withCount.totalBooks
                )
            }
        }

    val autoCollectionsEnabled: StateFlow<Boolean> =
        userPreferencesRepository.autoCollectionsEnabled
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), true)

    val collectionsState: StateFlow<UiState<List<CollectionUiState>>> = combine(
        userCollections,
        bookRepository.getAllBookPreviews(),
        autoCollectionsEnabled
    ) { collections, allBooks, smartEnabled ->
        if (!smartEnabled) {
            return@combine UiState.Success(collections)
        }

        val smartCollections = mutableListOf<CollectionUiState>()

        // 1. Recently Added (last 7 days)
        val sevenDaysAgo = System.currentTimeMillis() - (7 * 24 * 60 * 60 * 1000L)
        val recentlyAdded = allBooks.filter { it.addedAt >= sevenDaysAgo }
            .sortedByDescending { it.addedAt }
        if (recentlyAdded.isNotEmpty()) {
            smartCollections.add(
                CollectionUiState(
                    id = -1,
                    name = "Recently Added",
                    previews = recentlyAdded.take(8),
                    totalBooks = recentlyAdded.size,
                    isSmart = true
                )
            )
        }

        // 2. Abandoned (progress > 0% but not opened in 30+ days)
        val thirtyDaysAgo = System.currentTimeMillis() - (30 * 24 * 60 * 60 * 1000L)
        val abandoned = allBooks.filter {
            it.readingProgress in 1..99 && (it.lastOpenedAt ?: 0L) < thirtyDaysAgo
        }.sortedByDescending { it.lastOpenedAt }
        if (abandoned.isNotEmpty()) {
            smartCollections.add(
                CollectionUiState(
                    id = -2,
                    name = "Abandoned",
                    previews = abandoned.take(8),
                    totalBooks = abandoned.size,
                    isSmart = true
                )
            )
        }

        // 3. Finished [Year]
        val finishedBooks = allBooks.filter { it.finishedAt != null || it.readingProgress == 100 }
        val calendar = Calendar.getInstance()
        val finishedByYear = finishedBooks.groupBy {
            calendar.timeInMillis = it.finishedAt ?: it.lastOpenedAt ?: it.addedAt
            calendar.get(Calendar.YEAR)
        }.toSortedMap(compareByDescending { it })

        finishedByYear.forEach { (year, books) ->
            smartCollections.add(
                CollectionUiState(
                    id = -100L - year,
                    name = "Finished $year",
                    previews = books.take(8),
                    totalBooks = books.size,
                    isSmart = true
                )
            )
        }

        UiState.Success(smartCollections + collections)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), UiState.Loading)

    // Keep the old one for compatibility if needed, but ideally we migrate usages
    val collections: StateFlow<List<CollectionUiState>> = collectionsState
        .map { state -> (state as? UiState.Success)?.data ?: emptyList() }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun createCollection(name: String) {
        launchSafely(
            actionName = "createCollection",
            dispatcher = Dispatchers.IO,
            parameters = mapOf("name" to name.trim()),
        ) {
            collectionRepository.insertCollection(Collection(name = name.trim()))
        }
    }

    fun renameCollection(id: Long, name: String) {
        launchSafely(
            actionName = "renameCollection",
            dispatcher = Dispatchers.IO,
            parameters = mapOf("id" to id, "name" to name.trim()),
        ) {
            collectionRepository.renameCollection(id, name.trim())
        }
    }

    fun deleteCollection(id: Long) {
        launchSafely(
            actionName = "deleteCollection",
            dispatcher = Dispatchers.IO,
            parameters = mapOf("id" to id),
        ) {
            collectionRepository.deleteCollection(id)
        }
    }

    fun addBookToCollection(collectionId: Long, bookId: String) {
        launchSafely(
            actionName = "addBookToCollection",
            dispatcher = Dispatchers.IO,
            parameters = mapOf("collectionId" to collectionId, "bookId" to bookId),
        ) {
            collectionRepository.addBookToCollection(BookCollection(collectionId, bookId))
        }
    }

    fun removeBookFromCollection(collectionId: Long, bookId: String) {
        launchSafely(
            actionName = "removeBookFromCollection",
            dispatcher = Dispatchers.IO,
            parameters = mapOf("collectionId" to collectionId, "bookId" to bookId),
        ) {
            collectionRepository.removeBookFromCollection(collectionId, bookId)
        }
    }

    fun setAutoCollectionsEnabled(enabled: Boolean) {
        launchSafely(
            actionName = "setAutoCollectionsEnabled",
            parameters = mapOf("enabled" to enabled),
        ) {
            userPreferencesRepository.setAutoCollectionsEnabled(enabled)
        }
    }
}
