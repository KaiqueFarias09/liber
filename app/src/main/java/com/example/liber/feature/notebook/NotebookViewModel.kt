package com.example.liber.feature.notebook

import android.app.Application
import android.net.Uri
import androidx.lifecycle.viewModelScope
import com.example.liber.core.logging.AppLogger
import com.example.liber.core.logging.BaseAndroidViewModel
import com.example.liber.core.util.UiState
import com.example.liber.data.model.Annotation
import com.example.liber.data.model.AnnotationType
import com.example.liber.data.model.BookPreview
import com.example.liber.data.model.Bookmark
import com.example.liber.data.repository.BookRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import javax.inject.Inject

@HiltViewModel
class NotebookViewModel @Inject constructor(
    application: Application,
    private val bookRepository: BookRepository,
    appLogger: AppLogger,
) : BaseAndroidViewModel(application, "NotebookViewModel", appLogger) {

    private val _filterType = MutableStateFlow(NotebookFilterType.ALL)
    val filterType: StateFlow<NotebookFilterType> = _filterType

    private val _filterBookId = MutableStateFlow<String?>(null) // null means all books
    val filterBookId: StateFlow<String?> = _filterBookId

    val books: StateFlow<List<BookPreview>> = combine(
        bookRepository.getAllBookPreviews(),
        bookRepository.getAllAnnotations(),
        bookRepository.getAllBookmarks()
    ) { allBooks, allAnnotations, allBookmarks ->
        val bookIdsWithData =
            (allAnnotations.map { it.bookId } + allBookmarks.map { it.bookId }).toSet()
        allBooks.filter { it.id in bookIdsWithData }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val uiState: StateFlow<UiState<List<BookNotebookData>>> = combine(
        bookRepository.getAllBookPreviews(),
        bookRepository.getAllAnnotations(),
        bookRepository.getAllBookmarks(),
        _filterType,
        _filterBookId
    ) { allBooks, allAnnotations, allBookmarks, type, bookId ->
        try {
            val filteredAnnotations = allAnnotations.filter { annotation ->
                val matchesType = when (type) {
                    NotebookFilterType.ALL -> true
                    NotebookFilterType.HIGHLIGHTS -> annotation.type == AnnotationType.HIGHLIGHT
                    NotebookFilterType.BOOKMARKS -> false
                }
                val matchesBook = bookId == null || annotation.bookId == bookId
                matchesType && matchesBook
            }

            val filteredBookmarks =
                if (type == NotebookFilterType.ALL || type == NotebookFilterType.BOOKMARKS) {
                    allBookmarks.filter { bookmark ->
                        bookId == null || bookmark.bookId == bookId
                    }
                } else emptyList()

            val notebookItems = (filteredAnnotations.map { it.toNotebookItem() } +
                    filteredBookmarks.map { it.toNotebookItem() })
                .sortedByDescending { it.createdAt }

            val groupedData = notebookItems.groupBy { it.bookId }
                .mapNotNull { (bid, items) ->
                    val book = allBooks.find { it.id == bid } ?: return@mapNotNull null
                    BookNotebookData(
                        bookId = bid,
                        bookTitle = book.title,
                        author = book.author,
                        coverUri = book.coverUri,
                        items = items
                    )
                }
                .sortedBy { it.bookTitle }

            UiState.Success(groupedData)
        } catch (e: Exception) {
            UiState.Error(
                com.example.liber.core.util.UiText.DynamicString(
                    e.message ?: "Unknown error"
                )
            )
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), UiState.Loading)

    fun setFilterType(type: NotebookFilterType) {
        _filterType.value = type
    }

    fun setFilterBook(bookId: String?) {
        _filterBookId.value = bookId
    }
}

enum class NotebookFilterType {
    ALL, HIGHLIGHTS, BOOKMARKS
}

data class BookNotebookData(
    val bookId: String,
    val bookTitle: String,
    val author: String?,
    val coverUri: Uri?,
    val items: List<NotebookNotebookItem>
)

sealed class NotebookNotebookItem {
    abstract val id: Long
    abstract val bookId: String
    abstract val createdAt: Long
    abstract val chapter: String?

    data class Highlight(
        override val id: Long,
        override val bookId: String,
        override val createdAt: Long,
        override val chapter: String?,
        val quote: String?,
        val note: String?,
        val type: AnnotationType,
        val color: Int
    ) : NotebookNotebookItem()

    data class BookmarkItem(
        override val id: Long,
        override val bookId: String,
        override val createdAt: Long,
        override val chapter: String?
    ) : NotebookNotebookItem()
}

fun Annotation.toNotebookItem(): NotebookNotebookItem = NotebookNotebookItem.Highlight(
    id = id,
    bookId = bookId,
    createdAt = createdAt,
    chapter = chapter,
    quote = text,
    note = note,
    type = type,
    color = color
)

fun Bookmark.toNotebookItem(): NotebookNotebookItem = NotebookNotebookItem.BookmarkItem(
    id = id,
    bookId = bookId,
    createdAt = createdAt,
    chapter = chapter
)
