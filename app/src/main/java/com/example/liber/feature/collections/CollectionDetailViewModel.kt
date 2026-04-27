package com.example.liber.feature.collections

import android.app.Application
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.viewModelScope
import com.example.liber.core.logging.AppLogger
import com.example.liber.core.logging.BaseAndroidViewModel
import com.example.liber.core.util.UiState
import com.example.liber.core.util.UiText
import com.example.liber.data.model.BookCollection
import com.example.liber.data.model.BookPreview
import com.example.liber.data.repository.BookRepository
import com.example.liber.data.repository.CollectionRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import java.util.Calendar
import javax.inject.Inject

data class CollectionDetailUiState(
    val id: Long,
    val name: String,
    val books: List<BookPreview>,
    val isSmart: Boolean = false,
)

@HiltViewModel
class CollectionDetailViewModel @Inject constructor(
    application: Application,
    private val collectionRepository: CollectionRepository,
    private val bookRepository: BookRepository,
    savedStateHandle: SavedStateHandle,
    appLogger: AppLogger,
) : BaseAndroidViewModel(application, "CollectionDetailViewModel", appLogger) {

    private val collectionId: Long = checkNotNull(savedStateHandle["collectionId"])

    val allBooks: StateFlow<List<BookPreview>> = bookRepository.getAllBookPreviews()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val collectionState: StateFlow<UiState<CollectionDetailUiState>> = if (collectionId < 0) {
        // Smart Collection
        allBooks.map { books ->
            val (name, filteredBooks) = when {
                collectionId == -1L -> {
                    val sevenDaysAgo = System.currentTimeMillis() - (7 * 24 * 60 * 60 * 1000L)
                    "Recently Added" to books.filter { it.addedAt >= sevenDaysAgo }
                        .sortedByDescending { it.addedAt }
                }

                collectionId == -2L -> {
                    val thirtyDaysAgo = System.currentTimeMillis() - (30 * 24 * 60 * 60 * 1000L)
                    "Abandoned" to books.filter {
                        it.readingProgress in 1..99 && (it.lastOpenedAt ?: 0L) < thirtyDaysAgo
                    }.sortedByDescending { it.lastOpenedAt }
                }

                collectionId <= -100L -> {
                    val year = (-collectionId - 100).toInt()
                    val calendar = Calendar.getInstance()
                    "Finished $year" to books.filter {
                        val finishedAt =
                            it.finishedAt ?: (if (it.readingProgress == 100) it.lastOpenedAt
                                ?: it.addedAt else null)
                        if (finishedAt != null) {
                            calendar.timeInMillis = finishedAt
                            calendar.get(Calendar.YEAR) == year
                        } else false
                    }.sortedByDescending { it.finishedAt ?: it.lastOpenedAt }
                }

                else -> "Unknown" to emptyList<BookPreview>()
            }

            UiState.Success(
                CollectionDetailUiState(
                    id = collectionId,
                    name = name,
                    books = filteredBooks,
                    isSmart = true
                )
            )
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), UiState.Loading)
    } else {
        // Normal Collection
        collectionRepository
            .getCollectionWithPreviews(collectionId)
            .map { map ->
                val withCount = map.keys.firstOrNull()
                if (withCount != null) {
                    UiState.Success(
                        CollectionDetailUiState(
                            id = withCount.collection.id,
                            name = withCount.collection.name,
                            books = map[withCount] ?: emptyList(),
                        )
                    )
                } else {
                    UiState.Error(UiText.DynamicString("Collection not found"))
                }
            }
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), UiState.Loading)
    }

    fun renameCollection(name: String) {
        launchSafely(
            actionName = "renameCollection",
            dispatcher = Dispatchers.IO,
            parameters = mapOf("id" to collectionId, "name" to name.trim()),
        ) {
            collectionRepository.renameCollection(collectionId, name.trim())
        }
    }

    fun deleteCollection() {
        launchSafely(
            actionName = "deleteCollection",
            dispatcher = Dispatchers.IO,
            parameters = mapOf("id" to collectionId),
        ) {
            collectionRepository.deleteCollection(collectionId)
        }
    }

    fun addBookToCollection(bookId: String) {
        launchSafely(
            actionName = "addBookToCollection",
            dispatcher = Dispatchers.IO,
            parameters = mapOf("collectionId" to collectionId, "bookId" to bookId),
        ) {
            collectionRepository.addBookToCollection(BookCollection(collectionId, bookId))
        }
    }

    fun removeBookFromCollection(bookId: String) {
        launchSafely(
            actionName = "removeBookFromCollection",
            dispatcher = Dispatchers.IO,
            parameters = mapOf("collectionId" to collectionId, "bookId" to bookId),
        ) {
            collectionRepository.removeBookFromCollection(collectionId, bookId)
        }
    }
}
