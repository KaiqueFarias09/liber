package com.example.liber.feature.audiobook.components

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.liber.data.model.BookPreview
import com.example.liber.feature.library.LibrarySortOption
import com.example.liber.feature.library.LibraryViewMode

@Composable
fun AudiobookGrid(
    audiobooks: List<BookPreview>,
    onBookClick: (BookPreview) -> Unit,
    onDeleteBook: (BookPreview) -> Unit,
    onShareBook: (BookPreview) -> Unit,
    onToggleWantToRead: (BookPreview) -> Unit,
    onToggleFinished: (BookPreview) -> Unit,
    onShowDetails: (BookPreview) -> Unit,
    modifier: Modifier = Modifier,
    activeBookId: String? = null,
    isPlaying: Boolean = false,
    viewMode: LibraryViewMode = LibraryViewMode.GRID,
    onViewModeChange: (LibraryViewMode) -> Unit = {},
    sortOption: LibrarySortOption = LibrarySortOption.RECENT,
    onSortOptionChange: (LibrarySortOption) -> Unit = {},
    header: @Composable () -> Unit = {},
) {
    val sortedAudiobooks = remember(audiobooks, sortOption) {
        when (sortOption) {
            LibrarySortOption.TITLE -> audiobooks.sortedBy { it.title.lowercase() }
            LibrarySortOption.AUTHOR -> audiobooks.sortedBy { it.author?.lowercase() ?: "" }
            LibrarySortOption.RECENT -> audiobooks.sortedWith(
                compareByDescending<BookPreview> { it.lastOpenedAt != null }
                    .thenByDescending { it.lastOpenedAt }
            )

            LibrarySortOption.PROGRESS -> audiobooks.sortedByDescending { it.readingProgress }
        }
    }

    val contentPadding = PaddingValues(horizontal = 24.dp, vertical = 8.dp)

    BoxWithConstraints(modifier = modifier.fillMaxSize()) {
        AnimatedContent(
            targetState = viewMode,
            transitionSpec = { fadeIn() togetherWith fadeOut() },
            label = "viewMode",
            modifier = Modifier.fillMaxSize(),
        ) { mode ->
            if (mode == LibraryViewMode.GRID) {
                LazyVerticalGrid(
                    columns = when {
                        maxWidth < 600.dp -> GridCells.Fixed(2)
                        else -> GridCells.Adaptive(160.dp)
                    },
                    contentPadding = contentPadding,
                    horizontalArrangement = Arrangement.spacedBy(16.dp),
                    verticalArrangement = Arrangement.spacedBy(24.dp),
                    modifier = Modifier.fillMaxSize()
                ) {
                    item(span = { GridItemSpan(maxLineSpan) }) {
                        header()
                    }
                    items(sortedAudiobooks, key = { it.id }) { book ->
                        AudiobookGridItem(
                            book = book,
                            onClick = { onBookClick(book) },
                            onDeleteBook = { onDeleteBook(book) },
                            onShareBook = { onShareBook(book) },
                            onToggleWantToRead = { onToggleWantToRead(book) },
                            onToggleFinished = { onToggleFinished(book) },
                            onShowDetails = { onShowDetails(book) },
                            isActive = book.id == activeBookId,
                            isPlaying = isPlaying
                        )
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
                    items(sortedAudiobooks, key = { it.id }) { book ->
                        AudiobookListItem(
                            book = book,
                            onClick = { onBookClick(book) },
                            onDeleteBook = { onDeleteBook(book) },
                            onShareBook = { onShareBook(book) },
                            onToggleWantToRead = { onToggleWantToRead(book) },
                            onToggleFinished = { onToggleFinished(book) },
                            onShowDetails = { onShowDetails(book) },
                            isActive = book.id == activeBookId,
                            isPlaying = isPlaying,
                        )
                    }
                }
            }
        }
    }
}

// ── Grid item ─────────────────────────────────────────────────────────────────
