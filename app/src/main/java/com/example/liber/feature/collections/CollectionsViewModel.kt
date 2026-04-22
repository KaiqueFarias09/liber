package com.example.liber.feature.collections

import android.app.Application
import androidx.lifecycle.viewModelScope
import com.example.liber.core.logging.AppLogger
import com.example.liber.core.logging.BaseAndroidViewModel
import com.example.liber.core.util.UiState
import com.example.liber.data.local.CollectionWithBooksRelation
import com.example.liber.data.model.Book
import com.example.liber.data.model.BookCollection
import com.example.liber.data.model.Collection
import com.example.liber.data.repository.CollectionRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

data class CollectionUiState(
    val id: Long,
    val name: String,
    val books: List<Book>,
)

@HiltViewModel
class CollectionsViewModel @Inject constructor(
    application: Application,
    private val collectionRepository: CollectionRepository,
    appLogger: AppLogger,
) : BaseAndroidViewModel(application, "CollectionsViewModel", appLogger) {

    val collectionsState: StateFlow<UiState<List<CollectionUiState>>> = collectionRepository
        .getAllCollectionsWithBooks()
        .map { relations ->
            val data = relations.map { it.toUiState() }
            UiState.Success(data)
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), UiState.Loading)

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

    private fun CollectionWithBooksRelation.toUiState() = CollectionUiState(
        id = collection.id,
        name = collection.name,
        books = books,
    )
}
