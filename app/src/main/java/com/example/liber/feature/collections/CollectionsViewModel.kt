package com.example.liber.feature.collections

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.liber.data.local.CollectionWithBooksRelation
import com.example.liber.data.model.Book
import com.example.liber.data.model.BookCollectionEntity
import com.example.liber.data.model.CollectionEntity
import com.example.liber.data.model.toBook
import com.example.liber.data.repository.CollectionRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
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
) : AndroidViewModel(application) {

    val collections: StateFlow<List<CollectionUiState>> = collectionRepository
        .getAllCollectionsWithBooks()
        .map { relations -> relations.map { it.toUiState() } }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun createCollection(name: String) {
        viewModelScope.launch(Dispatchers.IO) {
            collectionRepository.insertCollection(CollectionEntity(name = name.trim()))
        }
    }

    fun renameCollection(id: Long, name: String) {
        viewModelScope.launch(Dispatchers.IO) {
            collectionRepository.renameCollection(id, name.trim())
        }
    }

    fun deleteCollection(id: Long) {
        viewModelScope.launch(Dispatchers.IO) {
            collectionRepository.deleteCollection(id)
        }
    }

    fun addBookToCollection(collectionId: Long, bookId: String) {
        viewModelScope.launch(Dispatchers.IO) {
            collectionRepository.addBookToCollection(BookCollectionEntity(collectionId, bookId))
        }
    }

    fun removeBookFromCollection(collectionId: Long, bookId: String) {
        viewModelScope.launch(Dispatchers.IO) {
            collectionRepository.removeBookFromCollection(collectionId, bookId)
        }
    }

    fun getCollectionIdsForBook(bookId: String): Flow<List<Long>> =
        collectionRepository.getCollectionIdsForBook(bookId)

    private fun CollectionWithBooksRelation.toUiState() = CollectionUiState(
        id = collection.id,
        name = collection.name,
        books = books.map { it.toBook() },
    )
}

