package com.example.liber.ui.library

import android.net.Uri
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SecondaryScrollableTabRow
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRowDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.adamglin.PhosphorIcons
import com.adamglin.phosphoricons.Regular
import com.adamglin.phosphoricons.regular.Plus
import com.example.liber.R
import com.example.liber.data.Book
import com.example.liber.ui.collections.CollectionDetailScreen
import com.example.liber.ui.collections.CollectionUiState
import com.example.liber.ui.collections.CollectionsListScreen
import com.example.liber.ui.collections.CollectionsViewModel
import com.example.liber.ui.components.BookGrid
import com.example.liber.ui.components.EmptyState
import com.example.liber.ui.home.HomeViewModel
import com.example.liber.ui.theme.LiberTheme
import kotlinx.coroutines.launch

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
    val pagerState = rememberPagerState { 2 }
    val scope = rememberCoroutineScope()
    var selectedCollectionId by remember { mutableStateOf<Long?>(null) }
    val selectedCollection = collections.find { it.id == selectedCollectionId }

    val selectedTabIndex = pagerState.currentPage

    LaunchedEffect(selectedTabIndex) {
        if (selectedTabIndex == 0) {
            selectedCollectionId = null
        }
    }

    Box(modifier = modifier.fillMaxSize()) {
        Column(
            modifier = Modifier.fillMaxSize(),
        ) {
            LibraryHeader(onAddBooks = onAddBooks)

            SecondaryScrollableTabRow(
                selectedTabIndex = selectedTabIndex,
                containerColor = Color.Transparent,
                edgePadding = 24.dp,
                indicator = {
                    TabRowDefaults.SecondaryIndicator(
                        modifier = Modifier.tabIndicatorOffset(selectedTabIndex),
                        color = MaterialTheme.colorScheme.primary,
                        height = 1.dp
                    )
                },
                divider = {
                    HorizontalDivider(
                        color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.2f)
                    )
                },
                modifier = Modifier.fillMaxWidth()
            ) {
                Tab(
                    selected = selectedTabIndex == 0,
                    onClick = {
                        scope.launch {
                            pagerState.animateScrollToPage(0)
                        }
                    },
                    selectedContentColor = MaterialTheme.colorScheme.onBackground,
                    unselectedContentColor = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                    text = {
                        Text(
                            text = "Books",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = if (selectedTabIndex == 0) FontWeight.Bold else FontWeight.Normal
                        )
                    }
                )
                Tab(
                    selected = selectedTabIndex == 1,
                    onClick = {
                        scope.launch {
                            pagerState.animateScrollToPage(1)
                        }
                    },
                    selectedContentColor = MaterialTheme.colorScheme.onBackground,
                    unselectedContentColor = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                    text = {
                        Text(
                            text = "Collections",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = if (selectedTabIndex == 1) FontWeight.Bold else FontWeight.Normal
                        )
                    }
                )
            }

            Spacer(Modifier.height(8.dp))

            HorizontalPager(
                state = pagerState,
                modifier = Modifier.weight(1f),
                verticalAlignment = Alignment.Top,
            ) { page ->
                when (page) {
                    0 -> {
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
                                showAddToCollection = true,
                                onAddToCollection = onAddToCollection,
                                collections = collections,
                            )
                        }
                    }

                    1 -> {
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
