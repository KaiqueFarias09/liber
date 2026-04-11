package com.example.liber.feature.collections

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import com.adamglin.PhosphorIcons
import com.adamglin.phosphoricons.Regular
import com.adamglin.phosphoricons.regular.ArrowLeft
import com.adamglin.phosphoricons.regular.CaretRight
import com.adamglin.phosphoricons.regular.DotsThreeVertical
import com.adamglin.phosphoricons.regular.PencilSimple
import com.adamglin.phosphoricons.regular.Plus
import com.adamglin.phosphoricons.regular.Trash
import com.example.liber.R
import com.example.liber.core.designsystem.BookCover
import com.example.liber.core.designsystem.BookGrid
import com.example.liber.core.designsystem.CoverStyle
import com.example.liber.core.designsystem.EmptyState
import com.example.liber.core.designsystem.LiberContextMenuDivider
import com.example.liber.core.designsystem.LiberContextMenuItem
import com.example.liber.core.designsystem.LiberDialog
import com.example.liber.core.designsystem.LiberDropdownMenu
import com.example.liber.core.designsystem.LiberFAB
import com.example.liber.core.designsystem.LiberTextField
import com.example.liber.data.model.Book
import com.example.liber.feature.library.LibrarySortOption
import com.example.liber.feature.library.LibraryViewMode

// ── List screen ───────────────────────────────────────────────────────────────

@Composable
fun CollectionsListScreen(
    collections: List<CollectionUiState>,
    onCollectionClick: (CollectionUiState) -> Unit,
    onCreateCollection: (String) -> Unit,
) {
    var showCreateDialog by remember { mutableStateOf(false) }

    Scaffold(
        containerColor = Color.Transparent,
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        floatingActionButton = {
            if (collections.isNotEmpty()) {
                LiberFAB(
                    onClick = { showCreateDialog = true },
                    icon = PhosphorIcons.Regular.Plus,
                    contentDescription = "New collection",
                )
            }
        },
    ) { innerPadding ->
        if (collections.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding),
                contentAlignment = Alignment.Center
            ) {
                EmptyState(
                    title = "No collections yet",
                    subtitle = "Curate your reading by grouping books into collections.",
                    image = R.drawable.collections_empty,
                    actionLabel = "Create Collection",
                    onAction = { showCreateDialog = true },
                    modifier = Modifier.padding(horizontal = 24.dp)
                )
            }
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(
                    start = 24.dp,
                    top = 8.dp, // Match BookGrid's top padding
                    end = 24.dp,
                    bottom = 80.dp + innerPadding.calculateBottomPadding()
                ),
                verticalArrangement = Arrangement.spacedBy(16.dp),
            ) {
                items(collections, key = { it.id }) { collection ->
                    CollectionShelfRow(
                        collection = collection,
                        onClick = { onCollectionClick(collection) },
                    )
                }
            }
        }
    }

    if (showCreateDialog) {
        CollectionNameDialog(
            title = "New collection",
            initialName = "",
            onConfirm = { name ->
                onCreateCollection(name)
                showCreateDialog = false
            },
            onDismiss = { showCreateDialog = false },
        )
    }
}


// ── Collection shelf card ─────────────────────────────────────────────────────

@Composable
private fun CollectionShelfRow(
    collection: CollectionUiState,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface,
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
    ) {
        Column(
            modifier = Modifier.padding(
                top = 16.dp,
                start = 16.dp,
                end = 16.dp,
                bottom = 12.dp
            )
        ) {
            // Title row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = collection.name,
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onSurface,
                    )
                    Text(
                        text = "${collection.books.size} ${if (collection.books.size == 1) "book" else "books"}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                Box(
                    modifier = Modifier
                        .size(28.dp)
                        .background(
                            MaterialTheme.colorScheme.surfaceVariant,
                            CircleShape,
                        ),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        imageVector = PhosphorIcons.Regular.CaretRight,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(14.dp),
                    )
                }
            }

            Spacer(Modifier.height(16.dp))

            // Stacked covers
            StackedBookCovers(
                books = collection.books,
                modifier = Modifier.fillMaxWidth(),
            )
        }
    }
}

// ── Stacked covers ────────────────────────────────────────────────────────────

// redundant spineGradient removed as we now use BookCover style

@Composable
private fun StackedBookCovers(
    books: List<Book>,
    modifier: Modifier = Modifier,
) {
    val displayBooks = books.take(8)
    val extraCount = (books.size - 8).coerceAtLeast(0)

    if (displayBooks.isEmpty()) {
        Box(
            modifier = modifier
                .height(40.dp) // Maintain some height for the card layout
                .fillMaxWidth(),
        )
        return
    }

    val coverWidth = 72.dp
    val step = 42.dp

    // contentAlignment = BottomStart so every cover's bottom edge sits at the shelf
    // floor. offset(x) shifts each cover rightward graphically. clipToBounds trims
    // any cover that is taller than shelfHeight at the top.
    Box(
        modifier = modifier.clipToBounds(),
        contentAlignment = Alignment.BottomStart,
    ) {
        displayBooks.forEachIndexed { index, book ->
            BookCover(
                book = book,
                style = CoverStyle.SMALL,
                isActive = false,
                isPlaying = false,
                modifier = Modifier
                    .offset(x = step * index)
                    .width(coverWidth),
            )
        }
        if (extraCount > 0) {
            Box(
                modifier = Modifier
                    .offset(x = step * displayBooks.size)
                    .width(coverWidth)
                    .height(120.dp)
                    .background(MaterialTheme.colorScheme.surfaceVariant),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    text = "+$extraCount",
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}

// ShelfCoverItem deleted in favor of BookCover

// ── Detail screen ─────────────────────────────────────────────────────────────

@Composable
fun CollectionDetailScreen(
    collection: CollectionUiState,
    allBooks: List<Book>,
    onBack: () -> Unit,
    onRename: (String) -> Unit,
    onDelete: () -> Unit,
    onAddBook: (String) -> Unit,
    onRemoveBook: (String) -> Unit,
    onOpenBook: (Book) -> Unit,
    onShareBook: (Book) -> Unit,
    onToggleWantToRead: (Book) -> Unit,
    onToggleFinished: (Book) -> Unit,
    onRenameBook: (Book, String) -> Unit,
    viewMode: LibraryViewMode = LibraryViewMode.GRID,
    onViewModeChange: (LibraryViewMode) -> Unit = {},
    sortOption: LibrarySortOption = LibrarySortOption.RECENT,
    onSortOptionChange: (LibrarySortOption) -> Unit = {},
    activeAudiobookId: String? = null,
    isAudiobookPlaying: Boolean = false,
) {
    var showMenu by remember { mutableStateOf(false) }
    var showRenameDialog by remember { mutableStateOf(false) }
    var showDeleteDialog by remember { mutableStateOf(false) }
    var showAddBooksSheet by remember { mutableStateOf(false) }

    remember(collection.books) { collection.books.chunked(2) }

    Column(
        modifier = Modifier
            .fillMaxSize(),
    ) {
        // Header
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .statusBarsPadding()
                .padding(horizontal = 8.dp, vertical = 4.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
            ) {
                IconButton(onClick = onBack) {
                    Icon(
                        imageVector = PhosphorIcons.Regular.ArrowLeft,
                        contentDescription = "Back",
                        tint = MaterialTheme.colorScheme.onBackground,
                    )
                }
                Text(
                    text = collection.name,
                    style = MaterialTheme.typography.headlineMedium,
                    color = MaterialTheme.colorScheme.onBackground,
                )
            }
            Box {
                IconButton(onClick = { showMenu = true }) {
                    Icon(
                        imageVector = PhosphorIcons.Regular.DotsThreeVertical,
                        contentDescription = "More options",
                        tint = MaterialTheme.colorScheme.onBackground,
                    )
                }
                LiberDropdownMenu(
                    expanded = showMenu,
                    onDismissRequest = { showMenu = false },
                ) {
                    LiberContextMenuItem(
                        label = "Add books",
                        icon = PhosphorIcons.Regular.Plus,
                        onClick = { showMenu = false; showAddBooksSheet = true },
                    )
                    LiberContextMenuItem(
                        label = "Rename",
                        icon = PhosphorIcons.Regular.PencilSimple,
                        onClick = { showMenu = false; showRenameDialog = true },
                    )
                    LiberContextMenuDivider()
                    LiberContextMenuItem(
                        label = "Delete collection",
                        icon = PhosphorIcons.Regular.Trash,
                        destructive = true,
                        onClick = { showMenu = false; showDeleteDialog = true },
                    )
                }
            }
        }


        // Book grid
        if (collection.books.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp),
                contentAlignment = Alignment.Center,
            ) {
                EmptyState(
                    title = "This collection is empty",
                    subtitle = "Add books from your library to this collection.",
                    image = R.drawable.collections_empty,
                    actionLabel = "Add books",
                    onAction = { showAddBooksSheet = true },
                )
            }
        } else {
            BookGrid(
                books = collection.books,
                onBookClick = onOpenBook,
                onToggleWantToRead = onToggleWantToRead,
                onToggleFinished = onToggleFinished,
                onRenameBook = onRenameBook,
                onDeleteBook = { onRemoveBook(it.id) },
                onShareBook = onShareBook,
                modifier = Modifier.weight(1f),
                contentPadding = PaddingValues(horizontal = 24.dp, vertical = 8.dp),
                deleteLabel = "Remove from collection",
                confirmDelete = false,
                showAddToCollection = false,
                viewMode = viewMode,
                onViewModeChange = onViewModeChange,
                sortOption = sortOption,
                onSortOptionChange = onSortOptionChange,
                activeAudiobookId = activeAudiobookId,
                isAudiobookPlaying = isAudiobookPlaying,
            )
        }
    }

    if (showRenameDialog) {
        CollectionNameDialog(
            title = "Rename collection",
            initialName = collection.name,
            onConfirm = { name ->
                onRename(name)
                showRenameDialog = false
            },
            onDismiss = { showRenameDialog = false },
        )
    }

    if (showDeleteDialog) {
        DeleteCollectionDialog(
            collectionName = collection.name,
            onConfirm = {
                showDeleteDialog = false
                onDelete()
            },
            onDismiss = { showDeleteDialog = false },
        )
    }

    if (showAddBooksSheet) {
        AddBooksDialog(
            allBooks = allBooks,
            booksInCollection = collection.books,
            onAddBook = onAddBook,
            onDismiss = { showAddBooksSheet = false },
        )
    }
}

// ── Dialogs ───────────────────────────────────────────────────────────────────

@Composable
private fun CollectionNameDialog(
    title: String,
    initialName: String,
    onConfirm: (String) -> Unit,
    onDismiss: () -> Unit,
) {
    var name by remember { mutableStateOf(initialName) }
    val focusRequester = remember { FocusRequester() }

    LiberDialog(
        onDismissRequest = onDismiss,
        title = title,
        confirmLabel = "Save",
        onConfirm = { if (name.isNotBlank()) onConfirm(name) },
        confirmEnabled = name.isNotBlank(),
        dismissLabel = "Cancel",
        onDismiss = onDismiss,
    ) {
        LiberTextField(
            value = name,
            onValueChange = { name = it },
            label = "Name",
            singleLine = true,
            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
            keyboardActions = KeyboardActions(onDone = { if (name.isNotBlank()) onConfirm(name) }),
            modifier = Modifier
                .fillMaxWidth()
                .focusRequester(focusRequester)
                .onGloballyPositioned { focusRequester.requestFocus() },
        )
    }
}

@Composable
private fun DeleteCollectionDialog(
    collectionName: String,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit,
) {
    LiberDialog(
        onDismissRequest = onDismiss,
        title = "Delete collection?",
        confirmLabel = "Delete",
        onConfirm = onConfirm,
        dismissLabel = "Cancel",
        onDismiss = onDismiss,
    ) {
        Text(
            "\"$collectionName\" will be permanently removed. " +
                    "The books inside will remain in your library.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@Composable
private fun AddBooksDialog(
    allBooks: List<Book>,
    booksInCollection: List<Book>,
    onAddBook: (String) -> Unit,
    onDismiss: () -> Unit,
) {
    val collectionBookIds = remember(booksInCollection) { booksInCollection.map { it.id }.toSet() }
    val availableBooks = remember(allBooks, collectionBookIds) {
        allBooks.filter { it.id !in collectionBookIds }
    }

    LiberDialog(
        onDismissRequest = onDismiss,
        title = "Add books",
        confirmLabel = null,
        onConfirm = null,
        dismissLabel = "Done",
        onDismiss = onDismiss,
    ) {
        if (availableBooks.isEmpty()) {
            Text(
                "All your library books are already in this collection.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        } else {
            LazyColumn(modifier = Modifier.fillMaxWidth()) {
                items(availableBooks, key = { it.id }) { book ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                onAddBook(book.id)
                                onDismiss()
                            }
                            .padding(vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        BookCover(
                            book = book,
                            style = CoverStyle.SMALL,
                            isActive = false,
                            isPlaying = false,
                            modifier = Modifier
                                .size(if (book.isAudiobook) 36.dp else 36.dp, 54.dp),
                        )
                        Spacer(Modifier.width(12.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = book.title,
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurface,
                            )
                            book.author?.let {
                                Text(
                                    text = it,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
