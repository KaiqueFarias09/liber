package com.example.liber.feature.library.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.adamglin.PhosphorIcons
import com.adamglin.phosphoricons.Regular
import com.adamglin.phosphoricons.regular.Rows
import com.adamglin.phosphoricons.regular.SquaresFour
import com.example.liber.R
import com.example.liber.core.designsystem.AppErrorState
import com.example.liber.core.designsystem.BookGrid
import com.example.liber.core.designsystem.EditorialDropdown
import com.example.liber.core.designsystem.EmptyState
import com.example.liber.core.designsystem.Gambetta
import com.example.liber.core.util.UiState
import com.example.liber.core.util.UiText
import com.example.liber.data.model.BookPreview
import com.example.liber.feature.audiobook.components.AudiobookGrid
import com.example.liber.feature.collections.CollectionUiState
import com.example.liber.feature.library.CollectionSortOption
import com.example.liber.feature.library.LibraryFilterStatus
import com.example.liber.feature.library.LibrarySortOption
import com.example.liber.feature.library.LibraryViewMode

@Composable
fun LibraryFilterAndSortRow(
    currentTabIndex: Int,
    filterStatus: LibraryFilterStatus,
    onFilterStatusChange: (LibraryFilterStatus) -> Unit,
    booksSortOption: LibrarySortOption,
    onBooksSortOptionChange: (LibrarySortOption) -> Unit,
    booksViewMode: LibraryViewMode,
    onBooksViewModeChange: (LibraryViewMode) -> Unit,
    audiobooksSortOption: LibrarySortOption,
    onAudiobooksSortOptionChange: (LibrarySortOption) -> Unit,
    audiobooksViewMode: LibraryViewMode,
    onAudiobooksViewModeChange: (LibraryViewMode) -> Unit,
    collectionsSortOption: CollectionSortOption,
    onCollectionsSortOptionChange: (CollectionSortOption) -> Unit,
    autoCollectionsEnabled: Boolean,
    onAutoCollectionsToggle: (Boolean) -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 24.dp, vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        if (currentTabIndex == 2) {
            // Smart Collections Toggle for Collections Tab
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .clip(CircleShape)
                    .clickable { onAutoCollectionsToggle(!autoCollectionsEnabled) }
                    .padding(vertical = 4.dp)
            ) {
                Text(
                    text = "Smart Collections ",
                    style = MaterialTheme.typography.bodySmall.copy(fontFamily = Gambetta),
                    color = if (autoCollectionsEnabled) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text = if (autoCollectionsEnabled) "On" else "Off",
                    style = MaterialTheme.typography.labelSmall.copy(
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 1.sp
                    ),
                    color = if (autoCollectionsEnabled) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant.copy(
                        alpha = 0.5f
                    )
                )
            }

            // Sort Dropdown for Collections
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = "Sort: ",
                    style = MaterialTheme.typography.labelSmall.copy(
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 1.sp,
                        fontSize = 9.sp
                    ),
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
                )
                EditorialDropdown(
                    value = collectionsSortOption.name,
                    onValueChange = { onCollectionsSortOptionChange(CollectionSortOption.valueOf(it)) },
                    options = CollectionSortOption.entries.map { it.name },
                    labelProvider = { sortName ->
                        CollectionSortOption.valueOf(sortName).label.asString()
                    }
                )
            }
            return@Row
        }

        val sortOption = if (currentTabIndex == 0) booksSortOption else audiobooksSortOption
        val currentOnSortOptionChange =
            if (currentTabIndex == 0) onBooksSortOptionChange else onAudiobooksSortOptionChange
        val viewMode = if (currentTabIndex == 0) booksViewMode else audiobooksViewMode
        val currentOnViewModeChange =
            if (currentTabIndex == 0) onBooksViewModeChange else onAudiobooksViewModeChange

        // Sentence Filter
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                text = "Showing ",
                style = MaterialTheme.typography.bodySmall.copy(fontFamily = Gambetta),
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            EditorialDropdown(
                value = filterStatus.name,
                onValueChange = { onFilterStatusChange(LibraryFilterStatus.valueOf(it)) },
                options = LibraryFilterStatus.entries.map { it.name },
                labelProvider = { statusName ->
                    LibraryFilterStatus.valueOf(statusName).label.asString().lowercase()
                }
            )
            Text(
                text = " .",
                style = MaterialTheme.typography.bodySmall.copy(fontFamily = Gambetta),
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        Row(verticalAlignment = Alignment.CenterVertically) {
            // Sort Dropdown
            Text(
                text = "Sort: ",
                style = MaterialTheme.typography.labelSmall.copy(
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 1.sp,
                    fontSize = 9.sp
                ),
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
            )
            EditorialDropdown(
                value = sortOption.name,
                onValueChange = { currentOnSortOptionChange(LibrarySortOption.valueOf(it)) },
                options = LibrarySortOption.entries.map { it.name },
                labelProvider = { sortName ->
                    LibrarySortOption.valueOf(sortName).label.asString()
                }
            )

            Spacer(Modifier.width(12.dp))

            // View Mode Toggle
            Surface(
                color = MaterialTheme.colorScheme.surfaceContainerLowest,
                shape = MaterialTheme.shapes.extraSmall,
                border = androidx.compose.foundation.BorderStroke(
                    1.dp,
                    MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f)
                ),
                modifier = Modifier.padding(vertical = 4.dp)
            ) {
                Row(modifier = Modifier.padding(2.dp)) {
                    val iconModifier = Modifier
                        .size(24.dp)
                        .padding(4.dp)

                    IconButton(
                        onClick = { currentOnViewModeChange(LibraryViewMode.GRID) },
                        modifier = Modifier.size(24.dp)
                    ) {
                        Icon(
                            imageVector = PhosphorIcons.Regular.SquaresFour,
                            contentDescription = "Grid View",
                            modifier = iconModifier,
                            tint = if (viewMode == LibraryViewMode.GRID) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant.copy(
                                alpha = 0.4f
                            )
                        )
                    }
                    IconButton(
                        onClick = { currentOnViewModeChange(LibraryViewMode.LIST) },
                        modifier = Modifier.size(24.dp)
                    ) {
                        Icon(
                            imageVector = PhosphorIcons.Regular.Rows,
                            contentDescription = "List View",
                            modifier = iconModifier,
                            tint = if (viewMode == LibraryViewMode.LIST) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant.copy(
                                alpha = 0.4f
                            )
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun LibraryBooksTab(
    booksState: UiState<List<BookPreview>>,
    isAudiobook: Boolean,
    onBookClick: (BookPreview) -> Unit,
    onAddBooks: () -> Unit,
    onToggleWantToRead: (BookPreview) -> Unit,
    onToggleFinished: (BookPreview) -> Unit,
    onShowDetails: (BookPreview) -> Unit,
    onDeleteBook: (BookPreview) -> Unit,
    onShareBook: (BookPreview) -> Unit,
    onAddToCollection: (BookPreview, Long) -> Unit,
    collections: List<CollectionUiState>,
    viewMode: LibraryViewMode,
    onViewModeChange: (LibraryViewMode) -> Unit,
    sortOption: LibrarySortOption,
    onSortOptionChange: (LibrarySortOption) -> Unit,
    searchQuery: String,
    onClearSearch: () -> Unit,
    activeBookId: String?,
    isPlaying: Boolean,
) {
    when (booksState) {
        is UiState.Loading -> LoadingState()
        is UiState.Error -> AppErrorState(
            title = booksState.title,
            message = booksState.message,
        )

        is UiState.Success -> {
            val books = booksState.data.filter { it.isAudiobook == isAudiobook }
            val header = @Composable {
                if (books.isNotEmpty()) {
                    Text(
                        text = if (isAudiobook) pluralStringResource(
                            R.plurals.label_audiobooks,
                            books.size,
                            books.size
                        ) else pluralStringResource(R.plurals.label_books, books.size, books.size),
                        style = MaterialTheme.typography.labelSmall.copy(
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 1.5.sp,
                            fontSize = 9.sp
                        ),
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f),
                        modifier = Modifier.padding(vertical = 8.dp)
                    )
                }
            }

            Column(modifier = Modifier.fillMaxSize()) {
                if (books.isEmpty()) {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        if (searchQuery.isNotEmpty()) {
                            EmptyState(
                                title = UiText.StringResource(R.string.error_no_results),
                                subtitle = UiText.StringResource(R.string.reader_search_no_results_subtitle),
                                image = R.drawable.library_empty,
                                actionLabel = UiText.DynamicString("Clear search"),
                                onAction = onClearSearch,
                                modifier = Modifier.padding(horizontal = 24.dp),
                            )
                        } else {
                            EmptyState(
                                title = if (isAudiobook) UiText.StringResource(R.string.empty_audiobooks_title)
                                else UiText.StringResource(R.string.empty_library_title),
                                subtitle = if (isAudiobook) UiText.StringResource(R.string.empty_audiobooks_subtitle)
                                else UiText.StringResource(R.string.empty_library_subtitle),
                                image = if (isAudiobook) R.drawable.audiobooks_empty else R.drawable.library_empty,
                                actionLabel = if (isAudiobook) UiText.StringResource(R.string.empty_audiobooks_action)
                                else UiText.StringResource(R.string.empty_library_action),
                                onAction = onAddBooks,
                                modifier = Modifier.padding(horizontal = 24.dp),
                            )
                        }
                    }
                } else {
                    if (isAudiobook) {
                        AudiobookGrid(
                            audiobooks = books,
                            onBookClick = onBookClick,
                            onDeleteBook = onDeleteBook,
                            onShareBook = onShareBook,
                            onToggleWantToRead = onToggleWantToRead,
                            onToggleFinished = onToggleFinished,
                            onShowDetails = onShowDetails,
                            activeBookId = activeBookId,
                            isPlaying = isPlaying,
                            viewMode = viewMode,
                            onViewModeChange = onViewModeChange,
                            sortOption = sortOption,
                            onSortOptionChange = onSortOptionChange,
                            header = header
                        )
                    } else {
                        BookGrid(
                            books = books,
                            onBookClick = onBookClick,
                            onToggleWantToRead = onToggleWantToRead,
                            onToggleFinished = onToggleFinished,
                            onShowDetails = onShowDetails,
                            onDeleteBook = onDeleteBook,
                            onShareBook = onShareBook,
                            showAddToCollection = true,
                            onAddToCollection = onAddToCollection,
                            collections = collections,
                            viewMode = viewMode,
                            onViewModeChange = onViewModeChange,
                            sortOption = sortOption,
                            onSortOptionChange = onSortOptionChange,
                            activeAudiobookId = activeBookId,
                            isAudiobookPlaying = isPlaying,
                            header = header
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun LoadingState() {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        androidx.compose.material3.CircularProgressIndicator()
    }
}
