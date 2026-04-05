package com.example.liber.ui.collections

import android.app.Application
import android.net.Uri
import androidx.core.net.toUri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.liber.data.AppDatabase
import com.example.liber.data.Book
import com.example.liber.data.BookCollectionEntity
import com.example.liber.data.BookEntity
import com.example.liber.data.CollectionEntity
import com.example.liber.data.CollectionWithBooksRelation
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.io.File

data class CollectionUiState(
    val id: Long,
    val name: String,
    val books: List<Book>,
)

class CollectionsViewModel(application: Application) : AndroidViewModel(application) {

    private val collectionDao = AppDatabase.getDatabase(application).collectionDao()

    val collections: StateFlow<List<CollectionUiState>> = collectionDao
        .getAllCollectionsWithBooks()
        .map { relations -> relations.map { it.toUiState() } }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun createCollection(name: String) {
        viewModelScope.launch(Dispatchers.IO) {
            collectionDao.insertCollection(CollectionEntity(name = name.trim()))
        }
    }

    fun renameCollection(id: Long, name: String) {
        viewModelScope.launch(Dispatchers.IO) {
            collectionDao.renameCollection(id, name.trim())
        }
    }

    fun deleteCollection(id: Long) {
        viewModelScope.launch(Dispatchers.IO) {
            collectionDao.deleteCollection(id)
        }
    }

    fun addBookToCollection(collectionId: Long, bookId: String) {
        viewModelScope.launch(Dispatchers.IO) {
            collectionDao.addBookToCollection(BookCollectionEntity(collectionId, bookId))
        }
    }

    fun removeBookFromCollection(collectionId: Long, bookId: String) {
        viewModelScope.launch(Dispatchers.IO) {
            collectionDao.removeBookFromCollection(collectionId, bookId)
        }
    }

    fun getCollectionIdsForBook(bookId: String): Flow<List<Long>> =
        collectionDao.getCollectionIdsForBook(bookId)

    private fun CollectionWithBooksRelation.toUiState() = CollectionUiState(
        id = collection.id,
        name = collection.name,
        books = books.map { it.toBook() },
    )
}

internal fun BookEntity.toBook() = Book(
    id = id,
    title = title,
    author = author,
    coverUri = coverPath?.let { Uri.fromFile(File(it)) },
    fileUri = fileUri.toUri(),
    lastOpenedAt = lastOpenedAt,
    wantToRead = wantToRead,
    readingProgress = readingProgress,
    lastLocator = lastLocator,
)
