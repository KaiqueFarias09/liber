package com.example.liber.core.designsystem

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
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
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.adamglin.PhosphorIcons
import com.adamglin.phosphoricons.Regular
import com.adamglin.phosphoricons.regular.CaretDown
import com.adamglin.phosphoricons.regular.Check
import com.adamglin.phosphoricons.regular.DotsThree
import com.adamglin.phosphoricons.regular.Rows
import com.adamglin.phosphoricons.regular.SquaresFour
import com.example.liber.data.model.Book
import com.example.liber.feature.collections.CollectionUiState
import com.example.liber.feature.collections.components.AddToCollectionDialog
import com.example.liber.feature.library.LibrarySortOption
import com.example.liber.feature.library.LibraryViewMode

@Composable
fun BookGrid(
    books: List<Book>,
    onBookClick: (Book) -> Unit,
    onToggleWantToRead: (Book) -> Unit,
    onToggleFinished: (Book) -> Unit,
    onRenameBook: (Book, String) -> Unit,
    onDeleteBook: (Book) -> Unit,
    onShareBook: (Book) -> Unit,
    modifier: Modifier = Modifier,
    contentPadding: PaddingValues = PaddingValues(horizontal = 24.dp, vertical = 8.dp),
    deleteLabel: String = "Delete…",
    confirmDelete: Boolean = true,
    showAddToCollection: Boolean = false,
    onAddToCollection: (Book, Long) -> Unit = { _, _ -> },
    collections: List<CollectionUiState> = emptyList(),
    viewMode: LibraryViewMode = LibraryViewMode.GRID,
    onViewModeChange: (LibraryViewMode) -> Unit = {},
    sortOption: LibrarySortOption = LibrarySortOption.RECENT,
    onSortOptionChange: (LibrarySortOption) -> Unit = {},
    activeAudiobookId: String? = null,
    isAudiobookPlaying: Boolean = false,
) {
    val sortedBooks = remember(books, sortOption) {
        when (sortOption) {
            LibrarySortOption.TITLE -> books.sortedBy { it.title.lowercase() }
            LibrarySortOption.AUTHOR -> books.sortedBy { it.author?.lowercase() ?: "" }
            LibrarySortOption.RECENT -> books.sortedWith(
                compareByDescending<Book> { it.lastOpenedAt != null }
                    .thenByDescending { it.lastOpenedAt }
            )
        }
    }

    BoxWithConstraints(modifier = modifier.fillMaxSize()) {
        val columns = when {
            maxWidth < 600.dp -> 2
            maxWidth < 1100.dp -> 5
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
                            BooksToolbar(
                                bookCount = books.size,
                                sortOption = sortOption,
                                onSortChange = onSortOptionChange,
                                viewMode = viewMode,
                                onViewModeChange = onViewModeChange,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 4.dp),
                            )
                        }
                        items(chunkedBooks, key = { it.first().id }) { rowBooks ->
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(20.dp),
                                verticalAlignment = Alignment.Bottom,
                            ) {
                                rowBooks.forEach { book ->
                                    BookGridItem(
                                        book = book,
                                        onClick = { onBookClick(book) },
                                        onToggleWantToRead = { onToggleWantToRead(book) },
                                        onToggleFinished = { onToggleFinished(book) },
                                        onRenameBook = { onRenameBook(book, it) },
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
                            BooksToolbar(
                                bookCount = books.size,
                                sortOption = sortOption,
                                onSortChange = onSortOptionChange,
                                viewMode = viewMode,
                                onViewModeChange = onViewModeChange,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 4.dp),
                            )
                        }
                        items(sortedBooks, key = { it.id }) { book ->
                            BookListItem(
                                book = book,
                                onClick = { onBookClick(book) },
                                onToggleWantToRead = { onToggleWantToRead(book) },
                                onToggleFinished = { onToggleFinished(book) },
                                onRenameBook = { onRenameBook(book, it) },
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

// ── Toolbar ───────────────────────────────────────────────────────────────────

@Composable
private fun BooksToolbar(
    bookCount: Int,
    sortOption: LibrarySortOption,
    onSortChange: (LibrarySortOption) -> Unit,
    viewMode: LibraryViewMode,
    onViewModeChange: (LibraryViewMode) -> Unit,
    modifier: Modifier = Modifier,
) {
    var sortMenuExpanded by remember { mutableStateOf(false) }

    Row(
        modifier = modifier.height(40.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = "$bookCount ${if (bookCount == 1) "book" else "books"}",
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )

        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            // Sort dropdown
            Box {
                Row(
                    modifier = Modifier
                        .clip(RoundedCornerShape(6.dp))
                        .clickable { sortMenuExpanded = true }
                        .padding(horizontal = 8.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                ) {
                    Text(
                        text = "Sort: ",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Text(
                        text = sortOption.label,
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onBackground,
                    )
                    Icon(
                        imageVector = PhosphorIcons.Regular.CaretDown,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier
                            .size(12.dp)
                            .rotate(if (sortMenuExpanded) 180f else 0f),
                    )
                }

                LiberDropdownMenu(
                    expanded = sortMenuExpanded,
                    onDismissRequest = { sortMenuExpanded = false },
                ) {
                    LibrarySortOption.entries.forEach { option ->
                        val isActive = sortOption == option
                        LiberContextMenuItem(
                            label = option.label,
                            icon = if (isActive) PhosphorIcons.Regular.Check else null,
                            onClick = {
                                onSortChange(option)
                                sortMenuExpanded = false
                            },
                        )
                    }
                }
            }

            // Divider
            Box(
                modifier = Modifier
                    .width(1.dp)
                    .height(16.dp)
                    .background(MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f)),
            )

            // View mode toggle
            Surface(
                shape = RoundedCornerShape(8.dp),
                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                tonalElevation = 0.dp,
            ) {
                Row(modifier = Modifier.padding(2.dp)) {
                    ViewToggleButton(
                        selected = viewMode == LibraryViewMode.GRID,
                        onClick = { onViewModeChange(LibraryViewMode.GRID) },
                        icon = {
                            Icon(
                                imageVector = PhosphorIcons.Regular.SquaresFour,
                                contentDescription = "Grid view",
                                modifier = Modifier.size(16.dp),
                            )
                        },
                    )
                    ViewToggleButton(
                        selected = viewMode == LibraryViewMode.LIST,
                        onClick = { onViewModeChange(LibraryViewMode.LIST) },
                        icon = {
                            Icon(
                                imageVector = PhosphorIcons.Regular.Rows,
                                contentDescription = "List view",
                                modifier = Modifier.size(16.dp),
                            )
                        },
                    )
                }
            }
        }
    }
}

@Composable
private fun ViewToggleButton(
    selected: Boolean,
    onClick: () -> Unit,
    icon: @Composable () -> Unit,
) {
    val iconTint = if (selected) MaterialTheme.colorScheme.onBackground
    else MaterialTheme.colorScheme.onSurfaceVariant

    Surface(
        shape = RoundedCornerShape(6.dp),
        color = if (selected) MaterialTheme.colorScheme.surface else Color.Transparent,
        tonalElevation = if (selected) 2.dp else 0.dp,
        modifier = Modifier.clickable(onClick = onClick),
    ) {
        Box(
            modifier = Modifier
                .size(28.dp)
                .padding(6.dp),
            contentAlignment = Alignment.Center,
        ) {
            CompositionLocalProvider(LocalContentColor provides iconTint) {
                icon()
            }
        }
    }
}

// ── List item ─────────────────────────────────────────────────────────────────

@Composable
private fun BookListItem(
    book: Book,
    onClick: () -> Unit,
    onToggleWantToRead: () -> Unit,
    onToggleFinished: () -> Unit,
    onRenameBook: (String) -> Unit,
    onDeleteBook: () -> Unit,
    onShareBook: () -> Unit,
    deleteLabel: String = "Delete…",
    confirmDelete: Boolean = true,
    showAddToCollection: Boolean = false,
    onAddToCollection: (Long) -> Unit = {},
    collections: List<CollectionUiState> = emptyList(),
    isActive: Boolean = false,
    isPlaying: Boolean = false,
) {
    var showMenu by remember { mutableStateOf(false) }
    var showRenameDialog by remember { mutableStateOf(false) }
    var showCollectionPicker by remember { mutableStateOf(false) }
    var showDeleteDialog by remember { mutableStateOf(false) }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .clickable(onClick = onClick)
            .padding(horizontal = 8.dp, vertical = 8.dp),
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
            book.readingProgress == 100 -> "FINISHED"
            book.lastOpenedAt == null && book.readingProgress == 0 -> "NEW"
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
                onRename = { showRenameDialog = true },
                onDelete = {
                    if (confirmDelete) {
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

    if (showRenameDialog) {
        RenameBookDialog(
            currentTitle = book.title,
            onConfirm = { newTitle ->
                onRenameBook(newTitle)
                showRenameDialog = false
            },
            onDismiss = { showRenameDialog = false },
        )
    }

    if (showDeleteDialog) {
        DeleteBookConfirmationDialog(
            bookTitle = book.title,
            actionLabel = deleteLabel,
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
    book: Book,
    onClick: () -> Unit,
    onToggleWantToRead: () -> Unit,
    onToggleFinished: () -> Unit,
    onRenameBook: (String) -> Unit,
    onDeleteBook: () -> Unit,
    onShareBook: () -> Unit,
    deleteLabel: String = "Delete…",
    confirmDelete: Boolean = true,
    showAddToCollection: Boolean = false,
    onAddToCollection: (Long) -> Unit = {},
    collections: List<CollectionUiState> = emptyList(),
    modifier: Modifier = Modifier,
    isActive: Boolean = false,
    isPlaying: Boolean = false,
) {
    var showMenu by remember { mutableStateOf(false) }
    var showRenameDialog by remember { mutableStateOf(false) }
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
            val progressText = when {
                book.readingProgress == 100 -> "FINISHED"
                book.lastOpenedAt == null && book.readingProgress == 0 -> "NEW"
                else -> "${book.readingProgress}%"
            }

            if (progressText == "NEW") {
                Surface(
                    color = MaterialTheme.colorScheme.primary.copy(alpha = 0.1f),
                    shape = RoundedCornerShape(4.dp),
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
                    onRename = { showRenameDialog = true },
                    onDelete = {
                        if (confirmDelete) {
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

    if (showRenameDialog) {
        RenameBookDialog(
            currentTitle = book.title,
            onConfirm = { newTitle ->
                onRenameBook(newTitle)
                showRenameDialog = false
            },
            onDismiss = { showRenameDialog = false },
        )
    }

    if (showDeleteDialog) {
        DeleteBookConfirmationDialog(
            bookTitle = book.title,
            actionLabel = deleteLabel,
            onConfirm = {
                showDeleteDialog = false
                onDeleteBook()
            },
            onDismiss = { showDeleteDialog = false },
        )
    }
}
