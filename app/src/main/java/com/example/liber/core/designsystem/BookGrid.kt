package com.example.liber.core.designsystem

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.liber.R
import com.example.liber.core.util.UiText
import com.example.liber.data.model.BookPreview
import com.example.liber.feature.collections.CollectionUiState
import com.example.liber.feature.library.LibrarySortOption
import com.example.liber.feature.library.LibraryViewMode

@Composable
fun BookGrid(
    books: List<BookPreview>,
    onBookClick: (BookPreview) -> Unit,
    onToggleWantToRead: (BookPreview) -> Unit,
    onToggleFinished: (BookPreview) -> Unit,
    onShowDetails: (BookPreview) -> Unit,
    onDeleteBook: (BookPreview) -> Unit,
    onShareBook: (BookPreview) -> Unit,
    modifier: Modifier = Modifier,
    contentPadding: PaddingValues = PaddingValues(horizontal = 24.dp, vertical = 8.dp),
    deleteLabel: UiText? = UiText.StringResource(R.string.action_delete_ellipsis),
    confirmDelete: Boolean = true,
    showAddToCollection: Boolean = false,
    onAddToCollection: (BookPreview, Long) -> Unit = { _, _ -> },
    collections: List<CollectionUiState> = emptyList(),
    viewMode: LibraryViewMode = LibraryViewMode.GRID,
    onViewModeChange: (LibraryViewMode) -> Unit = {},
    sortOption: LibrarySortOption = LibrarySortOption.RECENT,
    onSortOptionChange: (LibrarySortOption) -> Unit = {},
    activeAudiobookId: String? = null,
    isAudiobookPlaying: Boolean = false,
    header: @Composable () -> Unit = {},
) {
    val sortedBooks = remember(books, sortOption) {
        when (sortOption) {
            LibrarySortOption.TITLE -> books.sortedBy { it.title.lowercase() }
            LibrarySortOption.AUTHOR -> books.sortedBy { it.author?.lowercase() ?: "" }
            LibrarySortOption.RECENT -> books.sortedWith(
                compareByDescending<BookPreview> { it.lastOpenedAt != null }
                    .thenByDescending { it.lastOpenedAt }
            )

            LibrarySortOption.PROGRESS -> books.sortedByDescending { it.readingProgress }
        }
    }

    BoxWithConstraints(modifier = modifier.fillMaxSize()) {
        val columns = when {
            maxWidth < 600.dp -> 2
            maxWidth < MaxContentWidth -> 4
            maxWidth < 1200.dp -> 6
            else -> 8
        }

        val chunkedBooks = remember(sortedBooks, columns) { sortedBooks.chunked(columns) }

        Box(modifier = Modifier.fillMaxSize()) {
            AnimatedContent(
                targetState = viewMode,
                transitionSpec = { fadeIn() togetherWith fadeOut() },
                label = "viewMode",
                modifier = Modifier.fillMaxSize(),
            ) { mode ->
                if (mode == LibraryViewMode.GRID) {
                    LazyColumn(
                        contentPadding = contentPadding,
                        verticalArrangement = Arrangement.spacedBy(32.dp),
                        modifier = Modifier.fillMaxSize(),
                    ) {
                        item {
                            header()
                        }
                        items(chunkedBooks, key = { it.first().id }) { rowBooks ->
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(20.dp),
                                verticalAlignment = Alignment.Bottom,
                            ) {
                                for (book in rowBooks) {
                                    BookGridItem(
                                        book = book,
                                        onClick = { onBookClick(book) },
                                        onToggleWantToRead = { onToggleWantToRead(book) },
                                        onToggleFinished = { onToggleFinished(book) },
                                        onShowDetails = { onShowDetails(book) },
                                        onDeleteBook = { onDeleteBook(book) },
                                        onShareBook = { onShareBook(book) },
                                        deleteLabel = deleteLabel,
                                        confirmDelete = confirmDelete,
                                        showAddToCollection = showAddToCollection,
                                        onAddToCollection = { collectionId ->
                                            onAddToCollection(book, collectionId)
                                        },
                                        collections = collections,
                                        modifier = Modifier.weight(1f),
                                        isActive = book.id == activeAudiobookId,
                                        isPlaying = isAudiobookPlaying,
                                    )
                                }
                                if (rowBooks.size < columns) {
                                    repeat(columns - rowBooks.size) {
                                        Spacer(modifier = Modifier.weight(1f))
                                    }
                                }
                            }
                        }
                    }
                } else {
                    LazyColumn(
                        contentPadding = contentPadding,
                        verticalArrangement = Arrangement.spacedBy(2.dp),
                        modifier = Modifier.fillMaxSize(),
                    ) {
                        item {
                            header()
                        }
                        items(sortedBooks, key = { it.id }) { book ->
                            BookListItem(
                                book = book,
                                onClick = { onBookClick(book) },
                                onToggleWantToRead = { onToggleWantToRead(book) },
                                onToggleFinished = { onToggleFinished(book) },
                                onShowDetails = { onShowDetails(book) },
                                onDeleteBook = { onDeleteBook(book) },
                                onShareBook = { onShareBook(book) },
                                deleteLabel = deleteLabel,
                                confirmDelete = confirmDelete,
                                showAddToCollection = showAddToCollection,
                                onAddToCollection = { collectionId ->
                                    onAddToCollection(book, collectionId)
                                },
                                collections = collections,
                                isActive = book.id == activeAudiobookId,
                                isPlaying = isAudiobookPlaying,
                            )
                        }
                    }
                }
            }
        }
    }
}

// ── List item ─────────────────────────────────────────────────────────────────
