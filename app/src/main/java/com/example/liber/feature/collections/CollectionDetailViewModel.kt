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
import javax.inject.Inject

data class CollectionDetailUiState(
    val id: Long,
    val name: String,
    val books: List<BookPreview>,
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

    val collectionState: StateFlow<UiState<CollectionDetailUiState>> = collectionRepository
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

    val allBooks: StateFlow<List<BookPreview>> = bookRepository.getAllBookPreviews()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

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
