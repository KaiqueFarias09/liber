package com.example.liber.ui.collections

import android.net.Uri
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
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
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.adamglin.PhosphorIcons
import com.adamglin.phosphoricons.Regular
import com.adamglin.phosphoricons.regular.ArrowLeft
import com.adamglin.phosphoricons.regular.CaretRight
import com.adamglin.phosphoricons.regular.DotsThreeVertical
import com.adamglin.phosphoricons.regular.PencilSimple
import com.adamglin.phosphoricons.regular.Plus
import com.adamglin.phosphoricons.regular.Trash
import com.example.liber.R
import com.example.liber.data.Book
import com.example.liber.ui.components.BookCover
import com.example.liber.ui.components.BookGrid
import com.example.liber.ui.components.CoverStyle
import com.example.liber.ui.components.EmptyState
import com.example.liber.ui.library.LibrarySortOption
import com.example.liber.ui.library.LibraryViewMode

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
        floatingActionButton = {
            if (collections.isNotEmpty()) {
                FloatingActionButton(
                    onClick = { showCreateDialog = true },
                    containerColor = MaterialTheme.colorScheme.primary,
                    contentColor = MaterialTheme.colorScheme.onPrimary,
                    shape = RoundedCornerShape(16.dp),
                ) {
                    Icon(PhosphorIcons.Regular.Plus, contentDescription = "New collection")
                }
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
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding),
                verticalArrangement = Arrangement.spacedBy(0.dp),
            ) {
                items(collections, key = { it.id }) { collection ->
                    CollectionShelfRow(
                        collection = collection,
                        onClick = { onCollectionClick(collection) },
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                    )
                }
                item { Spacer(Modifier.height(80.dp)) }
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
        Column(modifier = Modifier.padding(top = 16.dp, start = 16.dp, end = 16.dp, bottom = 12.dp)) {
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

private val spineGradient = Brush.horizontalGradient(
    colorStops = arrayOf(
        0.00f to Color(0x55FFFFFF),
        0.04f to Color(0x55000000),
        0.07f to Color(0x22FFFFFF),
        0.12f to Color.Transparent,
    ),
)

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
            ShelfCoverItem(
                coverUri = book.coverUri,
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
                    .clip(RoundedCornerShape(3.dp))
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

@Composable
private fun ShelfCoverItem(
    coverUri: Uri?,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(3.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant),
    ) {
        // fillMaxWidth + FillWidth lets the image show at its natural aspect ratio —
        // width is fixed by the modifier, height expands to match the image proportions.
        AsyncImage(
            model = coverUri,
            contentDescription = null,
            modifier = Modifier.fillMaxWidth(),
            contentScale = ContentScale.FillWidth,
        )
        Box(
            modifier = Modifier
                .matchParentSize()
                .background(brush = spineGradient),
        )
    }
}

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
            IconButton(onClick = onBack) {
                Icon(
                    imageVector = PhosphorIcons.Regular.ArrowLeft,
                    contentDescription = "Back",
                    tint = MaterialTheme.colorScheme.onBackground,
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
                DropdownMenu(
                    expanded = showMenu,
                    onDismissRequest = { showMenu = false },
                ) {
                    DropdownMenuItem(
                        text = { Text("Rename") },
                        leadingIcon = { Icon(PhosphorIcons.Regular.PencilSimple, null) },
                        onClick = { showMenu = false; showRenameDialog = true },
                    )
                    DropdownMenuItem(
                        text = {
                            Text(
                                "Delete collection",
                                color = MaterialTheme.colorScheme.error,
                            )
                        },
                        leadingIcon = {
                            Icon(
                                PhosphorIcons.Regular.Trash,
                                null,
                                tint = MaterialTheme.colorScheme.error,
                            )
                        },
                        onClick = { showMenu = false; showDeleteDialog = true },
                    )
                }
            }
        }

        // Info
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp, vertical = 8.dp),
        ) {
            Text(
                text = collection.name,
                style = MaterialTheme.typography.headlineMedium,
                color = MaterialTheme.colorScheme.onBackground,
            )
            Spacer(Modifier.height(4.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = "${collection.books.size} ${if (collection.books.size == 1) "book" else "books"}",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Spacer(Modifier.width(12.dp))
                TextButton(
                    onClick = { showAddBooksSheet = true },
                    contentPadding = androidx.compose.foundation.layout.PaddingValues(0.dp),
                ) {
                    Icon(
                        PhosphorIcons.Regular.Plus,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp),
                        tint = MaterialTheme.colorScheme.primary
                    )
                    Spacer(Modifier.width(4.dp))
                    Text(
                        text = "Add books",
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            }
        }

        Spacer(Modifier.height(8.dp))

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
                showAddToCollection = false,
                viewMode = viewMode,
                onViewModeChange = onViewModeChange,
                sortOption = sortOption,
                onSortOptionChange = onSortOptionChange,
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

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = {
            OutlinedTextField(
                value = name,
                onValueChange = { name = it },
                label = { Text("Name") },
                singleLine = true,
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                keyboardActions = KeyboardActions(onDone = { if (name.isNotBlank()) onConfirm(name) }),
                modifier = Modifier
                    .fillMaxWidth()
                    .focusRequester(focusRequester)
                    .onGloballyPositioned { focusRequester.requestFocus() },
            )
        },
        confirmButton = {
            TextButton(
                onClick = { onConfirm(name) },
                enabled = name.isNotBlank(),
            ) { Text("Save") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        },
    )
}

@Composable
private fun DeleteCollectionDialog(
    collectionName: String,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Delete collection?") },
        text = {
            Text(
                "\"$collectionName\" will be permanently removed. " +
                    "The books inside will remain in your library.",
                style = MaterialTheme.typography.bodyMedium,
            )
        },
        confirmButton = {
            TextButton(onClick = onConfirm) {
                Text("Delete", color = MaterialTheme.colorScheme.error)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        },
    )
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

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Add books") },
        text = {
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
                                coverUri = book.coverUri,
                                contentDescription = book.title,
                                style = CoverStyle.SMALL,
                                modifier = Modifier
                                    .width(36.dp)
                                    .height(54.dp)
                                    .clip(RoundedCornerShape(3.dp)),
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
        },
        confirmButton = {
            TextButton(onClick = onDismiss) { Text("Done") }
        },
    )
}
