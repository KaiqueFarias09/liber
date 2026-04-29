package com.example.liber.core.designsystem

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.adamglin.PhosphorIcons
import com.adamglin.phosphoricons.Regular
import com.adamglin.phosphoricons.regular.DotsThree
import com.example.liber.R
import com.example.liber.core.util.UiText
import com.example.liber.data.model.BookPreview
import com.example.liber.feature.collections.CollectionUiState
import com.example.liber.feature.collections.components.AddToCollectionDialog
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
            maxWidth < 840.dp -> 4
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

@Composable
private fun BookListItem(
    book: BookPreview,
    onClick: () -> Unit,
    onToggleWantToRead: () -> Unit,
    onToggleFinished: () -> Unit,
    onShowDetails: () -> Unit,
    onDeleteBook: () -> Unit,
    onShareBook: () -> Unit,
    deleteLabel: UiText? = UiText.StringResource(R.string.action_delete_ellipsis),
    confirmDelete: Boolean = true,
    showAddToCollection: Boolean = false,
    onAddToCollection: (Long) -> Unit = {},
    collections: List<CollectionUiState> = emptyList(),
    isActive: Boolean = false,
    isPlaying: Boolean = false,
) {
    var showMenu by remember { mutableStateOf(false) }
    var showCollectionPicker by remember { mutableStateOf(false) }
    var showDeleteDialog by remember { mutableStateOf(false) }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .clickable(onClick = onClick)
            .padding(vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        // Cover — fixed size so the gradient overlay never bleeds beyond the image
        Box(
            modifier = Modifier
                .width(64.dp)
                .aspectRatio(if (book.isAudiobook) 1f else 2f / 3f),
        ) {
            BookCover(
                book = book,
                style = CoverStyle.SMALL,
                isActive = isActive,
                isPlaying = isPlaying,
                modifier = Modifier.fillMaxSize(),
                fillBounds = true,
            )
        }

        // Title + author
        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(3.dp),
        ) {
            Text(
                text = book.title,
                style = MaterialTheme.typography.titleSmall,
                color = MaterialTheme.colorScheme.onBackground,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            if (!book.author.isNullOrBlank()) {
                Text(
                    text = book.author,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }

        // Progress
        val progressText = when {
            book.readingProgress == 100 -> stringResource(R.string.label_status_finished)
            book.lastOpenedAt == null && book.readingProgress == 0 -> stringResource(R.string.label_status_new)
            else -> "${book.readingProgress}%"
        }
        Text(
            text = progressText,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )

        // More options
        Box {
            IconButton(
                onClick = { showMenu = true },
                modifier = Modifier.size(32.dp),
            ) {
                Icon(
                    imageVector = PhosphorIcons.Regular.DotsThree,
                    contentDescription = "More options",
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            BookActionMenu(
                expanded = showMenu,
                book = book,
                onDismiss = { showMenu = false },
                onShare = onShareBook,
                onToggleWantToRead = onToggleWantToRead,
                onToggleFinished = onToggleFinished,
                onShowDetails = onShowDetails,
                onDelete = {
                    if (confirmDelete && deleteLabel != null) {
                        showDeleteDialog = true
                    } else {
                        onDeleteBook()
                    }
                },
                deleteLabel = deleteLabel,
                showAddToCollection = showAddToCollection,
                onAddToCollection = { showCollectionPicker = true },
            )
        }
    }

    if (showCollectionPicker) {
        AddToCollectionDialog(
            collections = collections,
            onCollectionSelected = { collectionId ->
                onAddToCollection(collectionId)
                showCollectionPicker = false
            },
            onDismiss = { showCollectionPicker = false },
        )
    }

    if (showDeleteDialog && deleteLabel != null) {
        DeleteBookConfirmationDialog(
            bookTitle = book.title,
            action = deleteLabel,
            onConfirm = {
                showDeleteDialog = false
                onDeleteBook()
            },
            onDismiss = { showDeleteDialog = false },
        )
    }
}

// ── Grid item ─────────────────────────────────────────────────────────────────

@Composable
fun BookGridItem(
    book: BookPreview,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    onToggleWantToRead: () -> Unit,
    onToggleFinished: () -> Unit,
    onShowDetails: () -> Unit,
    onDeleteBook: () -> Unit,
    onShareBook: () -> Unit,
    deleteLabel: UiText? = UiText.StringResource(R.string.action_delete_ellipsis),
    confirmDelete: Boolean = true,
    showAddToCollection: Boolean = false,
    onAddToCollection: (Long) -> Unit = {},
    collections: List<CollectionUiState> = emptyList(),
    isActive: Boolean = false,
    isPlaying: Boolean = false,
) {
    var showMenu by remember { mutableStateOf(false) }
    var showCollectionPicker by remember { mutableStateOf(false) }
    var showDeleteDialog by remember { mutableStateOf(false) }

    Column(
        modifier = modifier.clickable(onClick = onClick),
    ) {
        BookCover(
            book = book,
            style = if (book.isAudiobook) CoverStyle.LARGE else CoverStyle.SMALL,
            isActive = isActive,
            isPlaying = isPlaying,
            modifier = Modifier
                .fillMaxWidth()
                .wrapContentHeight(),
        )

        Spacer(modifier = Modifier.height(10.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            val isNew = book.lastOpenedAt == null && book.readingProgress == 0
            val progressText = when {
                book.readingProgress == 100 -> stringResource(R.string.label_status_finished)
                isNew -> stringResource(R.string.label_status_new)
                else -> "${book.readingProgress}%"
            }

            if (isNew) {
                Box(
                    modifier = Modifier.liberContainer(
                        shape = RoundedCornerShape(4.dp),
                        backgroundColor = MaterialTheme.colorScheme.surfaceContainerHighest
                    )
                ) {
                    Text(
                        text = progressText,
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp),
                    )
                }
            } else {
                Text(
                    text = progressText,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }

            Box {
                IconButton(
                    onClick = { showMenu = true },
                    modifier = Modifier.size(24.dp),
                ) {
                    Icon(
                        imageVector = PhosphorIcons.Regular.DotsThree,
                        contentDescription = "More options",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                BookActionMenu(
                    expanded = showMenu,
                    book = book,
                    onDismiss = { showMenu = false },
                    onShare = onShareBook,
                    onToggleWantToRead = onToggleWantToRead,
                    onToggleFinished = onToggleFinished,
                    onShowDetails = onShowDetails,
                    onDelete = {
                        if (confirmDelete && deleteLabel != null) {
                            showDeleteDialog = true
                        } else {
                            onDeleteBook()
                        }
                    },
                    deleteLabel = deleteLabel,
                    showAddToCollection = showAddToCollection,
                    onAddToCollection = { showCollectionPicker = true },
                )
            }
        }
    }

    if (showCollectionPicker) {
        AddToCollectionDialog(
            collections = collections,
            onCollectionSelected = { collectionId ->
                onAddToCollection(collectionId)
                showCollectionPicker = false
            },
            onDismiss = { showCollectionPicker = false },
        )
    }

    if (showDeleteDialog && deleteLabel != null) {
        DeleteBookConfirmationDialog(
            bookTitle = book.title,
            action = deleteLabel,
            onConfirm = {
                showDeleteDialog = false
                onDeleteBook()
            },
            onDismiss = { showDeleteDialog = false },
        )
    }
}
