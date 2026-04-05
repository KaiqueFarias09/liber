package com.example.liber.ui.library

import android.net.Uri
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SecondaryTabRow
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRowDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.adamglin.PhosphorIcons
import com.adamglin.phosphoricons.Regular
import com.adamglin.phosphoricons.regular.DotsThree
import com.adamglin.phosphoricons.regular.Plus
import com.adamglin.phosphoricons.regular.Stack
import com.example.liber.R
import com.example.liber.data.Book
import com.example.liber.ui.collections.CollectionDetailScreen
import com.example.liber.ui.collections.CollectionUiState
import com.example.liber.ui.collections.CollectionsListScreen
import com.example.liber.ui.collections.CollectionsViewModel
import com.example.liber.ui.components.BookActionMenu
import com.example.liber.ui.components.BookCover
import com.example.liber.ui.components.CoverStyle
import com.example.liber.ui.components.EmptyState
import com.example.liber.ui.home.HomeViewModel
import com.example.liber.ui.theme.LiberTheme

@Composable
fun LibraryScreen(
    books: List<Book>,
    isLoading: Boolean,
    onBookClick: (Book) -> Unit,
    onAddBooks: () -> Unit,
    onToggleWantToRead: (Book) -> Unit,
    onToggleFinished: (Book) -> Unit,
    onRenameBook: (Book, String) -> Unit,
    onDeleteBook: (Book) -> Unit,
    onShareBook: (Book) -> Unit,
    onAddToCollection: (Book, Long) -> Unit = { _, _ -> },
    collections: List<CollectionUiState> = emptyList(),
    onCreateCollection: (String) -> Unit = {},
    onRenameCollection: (Long, String) -> Unit = { _, _ -> },
    onDeleteCollection: (Long) -> Unit = {},
    onAddBookToCollection: (Long, String) -> Unit = { _, _ -> },
    onRemoveBookFromCollection: (Long, String) -> Unit = { _, _ -> },
    modifier: Modifier = Modifier,
) {
    var selectedTabIndex by remember { mutableIntStateOf(0) }
    var selectedCollectionId by remember { mutableStateOf<Long?>(null) }
    val selectedCollection = collections.find { it.id == selectedCollectionId }

    Box(modifier = modifier.fillMaxSize()) {
        Column(
            modifier = Modifier.fillMaxSize(),
        ) {
            LibraryHeader(onAddBooks = onAddBooks)

            SecondaryTabRow(
                selectedTabIndex = selectedTabIndex,
                containerColor = Color.Transparent,
                contentColor = MaterialTheme.colorScheme.onBackground,
                indicator = {
                    TabRowDefaults.SecondaryIndicator(
                        modifier = Modifier.tabIndicatorOffset(selectedTabIndex),
                        color = MaterialTheme.colorScheme.primary
                    )
                },
                divider = {},
                modifier = Modifier.padding(horizontal = 24.dp)
            ) {
                Tab(
                    selected = selectedTabIndex == 0,
                    onClick = { selectedTabIndex = 0; selectedCollectionId = null },
                    text = { Text("Books", style = MaterialTheme.typography.titleMedium) }
                )
                Tab(
                    selected = selectedTabIndex == 1,
                    onClick = { selectedTabIndex = 1 },
                    text = { Text("Collections", style = MaterialTheme.typography.titleMedium) }
                )
            }

            Spacer(Modifier.height(8.dp))

            AnimatedContent(
                targetState = if (selectedTabIndex == 0) "books" else "collections",
                transitionSpec = {
                    slideInHorizontally { it } togetherWith slideOutHorizontally { -it }
                },
                modifier = Modifier.weight(1f),
                label = "library_content"
            ) { state ->
                when (state) {
                    "books" -> {
                        when {
                            isLoading -> LoadingState()
                            books.isEmpty() -> Box(
                                modifier = Modifier.fillMaxSize(),
                                contentAlignment = Alignment.Center
                            ) {
                                EmptyState(
                                    title = "Your library is empty",
                                    subtitle = "Tap + to add books",
                                    image = R.drawable.library_empty,
                                    actionLabel = "Add Books",
                                    onAction = onAddBooks,
                                    modifier = Modifier.padding(horizontal = 24.dp),
                                )
                            }

                            else -> BookGrid(
                                books = books,
                                onBookClick = onBookClick,
                                onToggleWantToRead = onToggleWantToRead,
                                onToggleFinished = onToggleFinished,
                                onRenameBook = onRenameBook,
                                onDeleteBook = onDeleteBook,
                                onShareBook = onShareBook,
                                onAddToCollection = onAddToCollection,
                                collections = collections,
                            )
                        }
                    }

                    "collections" -> {
                        CollectionsListScreen(
                            collections = collections,
                            onCollectionClick = { selectedCollectionId = it.id },
                            onCreateCollection = onCreateCollection,
                        )
                    }
                }
            }
        }

        if (selectedCollectionId != null && selectedCollection != null) {
            Surface(
                modifier = Modifier.fillMaxSize(),
                color = MaterialTheme.colorScheme.background
            ) {
                CollectionDetailScreen(
                    collection = selectedCollection,
                    allBooks = books,
                    onBack = { selectedCollectionId = null },
                    onRename = { onRenameCollection(selectedCollection.id, it) },
                    onDelete = {
                        onDeleteCollection(selectedCollection.id)
                        selectedCollectionId = null
                    },
                    onAddBook = { onAddBookToCollection(selectedCollection.id, it) },
                    onRemoveBook = {
                        onRemoveBookFromCollection(
                            selectedCollection.id,
                            it
                        )
                    },
                    onOpenBook = onBookClick,
                    onShareBook = onShareBook,
                    onToggleWantToRead = onToggleWantToRead,
                    onToggleFinished = onToggleFinished,
                    onRenameBook = onRenameBook,
                )
            }
        }
    }
}

// ── Convenience overload that reads directly from the ViewModel ───────────────

@Composable
fun LibraryScreen(
    viewModel: HomeViewModel,
    onBookClick: (Book) -> Unit,
    onAddBooks: () -> Unit,
    onShareBook: (Book) -> Unit,
    collectionsViewModel: CollectionsViewModel,
    modifier: Modifier = Modifier,
) {
    val books by viewModel.books.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()
    val collections by collectionsViewModel.collections.collectAsState()

    LibraryScreen(
        books = books,
        isLoading = isLoading,
        onBookClick = onBookClick,
        onAddBooks = onAddBooks,
        onToggleWantToRead = { book -> viewModel.toggleWantToRead(book.id, book.wantToRead) },
        onToggleFinished = { book -> viewModel.toggleFinished(book.id, book.readingProgress == 100) },
        onRenameBook = { book, newTitle -> viewModel.renameBook(book.id, newTitle) },
        onDeleteBook = { book -> viewModel.deleteBook(book.id) },
        onShareBook = onShareBook,
        onAddToCollection = { book, collectionId ->
            collectionsViewModel.addBookToCollection(collectionId, book.id)
        },
        collections = collections,
        onCreateCollection = { collectionsViewModel.createCollection(it) },
        onRenameCollection = { id, name -> collectionsViewModel.renameCollection(id, name) },
        onDeleteCollection = { collectionsViewModel.deleteCollection(it) },
        onAddBookToCollection = { id, bookId ->
            collectionsViewModel.addBookToCollection(
                id,
                bookId
            )
        },
        onRemoveBookFromCollection = { id, bookId ->
            collectionsViewModel.removeBookFromCollection(
                id,
                bookId
            )
        },
        modifier = modifier,
    )
}

// ── Private sub-composables ───────────────────────────────────────────────────

@Composable
private fun LibraryHeader(onAddBooks: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .statusBarsPadding()
            .padding(start = 24.dp, end = 8.dp, top = 20.dp, bottom = 8.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = "Library",
            style = MaterialTheme.typography.headlineLarge,
            color = MaterialTheme.colorScheme.onBackground,
        )
        IconButton(onClick = onAddBooks) {
            Icon(
                imageVector = PhosphorIcons.Regular.Plus,
                contentDescription = "Add Books",
                tint = MaterialTheme.colorScheme.onBackground,
            )
        }
    }
}

@Composable
private fun LoadingState() {
    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
    }
}

@Composable
private fun BookGrid(
    books: List<Book>,
    onBookClick: (Book) -> Unit,
    onToggleWantToRead: (Book) -> Unit,
    onToggleFinished: (Book) -> Unit,
    onRenameBook: (Book, String) -> Unit,
    onDeleteBook: (Book) -> Unit,
    onShareBook: (Book) -> Unit,
    onAddToCollection: (Book, Long) -> Unit = { _, _ -> },
    collections: List<CollectionUiState> = emptyList(),
) {
    val chunkedBooks = remember(books) { books.chunked(2) }

    LazyColumn(
        contentPadding = PaddingValues(horizontal = 24.dp, vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(24.dp),
        modifier = Modifier.fillMaxSize()
    ) {
        items(chunkedBooks, key = { it.first().id }) { rowBooks ->
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(16.dp),
                verticalAlignment = Alignment.Bottom
            ) {
                rowBooks.forEach { book ->
                    Box(
                        modifier = Modifier.weight(1f),
                        contentAlignment = Alignment.BottomStart
                    ) {
                        LibraryBookItem(
                            book = book,
                            onClick = { onBookClick(book) },
                            onToggleWantToRead = { onToggleWantToRead(book) },
                            onToggleFinished = { onToggleFinished(book) },
                            onRenameBook = { onRenameBook(book, it) },
                            onDeleteBook = { onDeleteBook(book) },
                            onShareBook = { onShareBook(book) },
                            onAddToCollection = { collectionId -> onAddToCollection(book, collectionId) },
                            collections = collections,
                        )
                    }
                }
                if (rowBooks.size == 1) {
                    Spacer(modifier = Modifier.weight(1f))
                }
            }
        }
    }
}

@Composable
private fun LibraryBookItem(
    book: Book,
    onClick: () -> Unit,
    onToggleWantToRead: () -> Unit,
    onToggleFinished: () -> Unit,
    onRenameBook: (String) -> Unit,
    onDeleteBook: () -> Unit,
    onShareBook: () -> Unit,
    onAddToCollection: (Long) -> Unit = {},
    collections: List<CollectionUiState> = emptyList(),
) {
    var showMenu by remember { mutableStateOf(false) }
    var showRenameDialog by remember { mutableStateOf(false) }
    var showCollectionPicker by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .widthIn(max = 180.dp)
            .clickable(onClick = onClick),
    ) {
        BookCover(
            coverUri = book.coverUri,
            contentDescription = book.title,
            style = CoverStyle.LARGE,
            modifier = Modifier
                .fillMaxWidth()
                .wrapContentHeight(),
        )
        
        Spacer(modifier = Modifier.height(8.dp))
        
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Progress Label
            val progressText = when {
                book.readingProgress == 100 -> "FINISHED"
                book.lastOpenedAt == null && book.readingProgress == 0 -> "NEW"
                else -> "${book.readingProgress}%"
            }

            if (progressText == "NEW") {
                Surface(
                    color = MaterialTheme.colorScheme.primary.copy(alpha = 0.1f),
                    shape = RoundedCornerShape(4.dp)
                ) {
                    Text(
                        text = progressText,
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp)
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
                    modifier = Modifier.size(24.dp)
                ) {
                    Icon(
                        imageVector = PhosphorIcons.Regular.DotsThree,
                        contentDescription = "More options",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
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
                    onDelete = onDeleteBook,
                    deleteLabel = "Remove…",
                    showAddToCollection = true,
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
}

@Composable
private fun AddToCollectionDialog(
    collections: List<CollectionUiState>,
    onCollectionSelected: (Long) -> Unit,
    onDismiss: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Add to Collection") },
        text = {
            if (collections.isEmpty()) {
                Text(
                    "You have no collections yet. Create one in the Collections tab.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            } else {
                LazyColumn(modifier = Modifier.fillMaxWidth()) {
                    items(collections, key = { it.id }) { collection ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { onCollectionSelected(collection.id) }
                                .padding(vertical = 12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Icon(
                                imageVector = PhosphorIcons.Regular.Stack,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(20.dp),
                            )
                            Spacer(Modifier.width(12.dp))
                            Column {
                                Text(
                                    text = collection.name,
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurface,
                                )
                                Text(
                                    text = "${collection.books.size} ${if (collection.books.size == 1) "book" else "books"}",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        },
    )
}

@Composable
private fun RenameBookDialog(
    currentTitle: String,
    onConfirm: (String) -> Unit,
    onDismiss: () -> Unit,
) {
    var title by remember { mutableStateOf(currentTitle) }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Rename book") },
        text = {
            androidx.compose.material3.OutlinedTextField(
                value = title,
                onValueChange = { title = it },
                label = { Text("Title") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
            )
        },
        confirmButton = {
            TextButton(onClick = { onConfirm(title) }, enabled = title.isNotBlank()) {
                Text("Save")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        },
    )
}

// ── Previews ──────────────────────────────────────────────────────────────────

private val previewBooks = listOf(
    Book("1", "Lean UX", "Jeff Gothelf", null, Uri.EMPTY),
    Book("2", "Emotional Design", "Donald A. Norman", null, Uri.EMPTY, wantToRead = true),
    Book("3", "100 Things Every Designer Needs to Know", "Susan Weinschenk", null, Uri.EMPTY),
    Book("4", "The Design of Everyday Things", "Don Norman", null, Uri.EMPTY),
)

@Preview(showBackground = true, heightDp = 800)
@Composable
private fun LibraryScreenPreview() {
    LiberTheme {
        LibraryScreen(
            books = previewBooks,
            isLoading = false,
            onBookClick = {},
            onAddBooks = {},
            onToggleWantToRead = {},
            onToggleFinished = {},
            onRenameBook = { _, _ -> },
            onDeleteBook = {},
            onShareBook = {},
        )
    }
}

@Preview(showBackground = true, heightDp = 600)
@Composable
private fun LibraryScreenEmptyPreview() {
    LiberTheme {
        LibraryScreen(
            books = emptyList(),
            isLoading = false,
            onBookClick = {},
            onAddBooks = {},
            onToggleWantToRead = {},
            onToggleFinished = {},
            onRenameBook = { _, _ -> },
            onDeleteBook = {},
            onShareBook = {},
        )
    }
}
