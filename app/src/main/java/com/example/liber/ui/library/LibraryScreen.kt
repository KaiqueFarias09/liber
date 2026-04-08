package com.example.liber.ui.library

import android.net.Uri
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.nestedscroll.NestedScrollConnection
import androidx.compose.ui.input.nestedscroll.NestedScrollSource
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.example.liber.R
import com.example.liber.data.Book
import com.example.liber.data.ScanState
import com.example.liber.ui.collections.CollectionDetailScreen
import com.example.liber.ui.collections.CollectionUiState
import com.example.liber.ui.collections.CollectionsListScreen
import com.example.liber.ui.collections.CollectionsViewModel
import com.example.liber.ui.components.AudiobookGrid
import com.example.liber.ui.components.BookGrid
import com.example.liber.ui.components.EmptyState
import com.example.liber.ui.components.LiberHeader
import com.example.liber.ui.components.LiberTabBar
import com.example.liber.ui.components.ScanProgressBanner
import com.example.liber.ui.home.HomeViewModel
import com.example.liber.ui.theme.LiberTheme
import kotlinx.coroutines.launch

@Composable
fun LibraryScreen(
    books: List<Book>,
    isLoading: Boolean,
    onBookClick: (Book) -> Unit,
    onAddBooks: () -> Unit,
    scanState: ScanState = ScanState.Idle,
    onDismissScanBanner: () -> Unit = {},
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
    viewMode: LibraryViewMode = LibraryViewMode.GRID,
    onViewModeChange: (LibraryViewMode) -> Unit = {},
    sortOption: LibrarySortOption = LibrarySortOption.RECENT,
    onSortOptionChange: (LibrarySortOption) -> Unit = {},
    selectedCollectionId: Long? = null,
    onCollectionClick: (Long?) -> Unit = {},
    modifier: Modifier = Modifier,
) {
    val pagerState = rememberPagerState { 3 }
    val scope = rememberCoroutineScope()
    val selectedCollection = collections.find { it.id == selectedCollectionId }

    val selectedTabIndex = pagerState.currentPage

    LaunchedEffect(selectedTabIndex) {
        if (selectedTabIndex == 2) { // Changed to 2 for Collections, because Audiobooks is 1
            // if we want Collections selection logic to run when tab 0 or 1 is selected
            // we should adjust it. The original code did: `if (selectedTabIndex == 0) onCollectionClick(null)`
        }
        if (selectedTabIndex != 2) {
            onCollectionClick(null)
        }
    }

    val density = LocalDensity.current
    val headerHeightState = remember { mutableIntStateOf(0) }
    val scrolledPxState = remember { mutableFloatStateOf(0f) }

    val nestedScrollConnection = remember {
        object : NestedScrollConnection {
            override fun onPreScroll(available: Offset, source: NestedScrollSource): Offset {
                val delta = available.y
                val maxScroll = headerHeightState.intValue.toFloat()
                val oldScrolled = scrolledPxState.floatValue
                scrolledPxState.floatValue = (oldScrolled - delta).coerceIn(0f, maxScroll)
                val consumed = -(scrolledPxState.floatValue - oldScrolled)
                return Offset(0f, consumed)
            }
        }
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .nestedScroll(nestedScrollConnection)
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            Spacer(
                Modifier
                    .fillMaxWidth()
                    .height(
                        with(density) {
                            (headerHeightState.intValue.toFloat() - scrolledPxState.floatValue)
                                .coerceAtLeast(0f).toDp()
                        }
                    )
            )

            ScanProgressBanner(
                state = scanState,
                onDismiss = onDismissScanBanner,
            )

            LiberTabBar(
                tabs = listOf("Books", "Audiobooks", "Collections"),
                selectedTabIndex = selectedTabIndex,
                onTabSelected = { index ->
                    scope.launch {
                        pagerState.animateScrollToPage(index)
                    }
                }
            )

            Spacer(Modifier.height(8.dp))

            val audiobooks = remember(books) { books.filter { it.mediaType == "audio/mpeg" } }
            val regularBooks = remember(books) { books.filter { it.mediaType != "audio/mpeg" } }

            HorizontalPager(
                state = pagerState,
                modifier = Modifier.weight(1f),
                verticalAlignment = Alignment.Top,
            ) { page ->
                when (page) {
                    0 -> {
                        when {
                            isLoading -> LoadingState()
                            regularBooks.isEmpty() -> Box(
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
                                books = regularBooks,
                                onBookClick = onBookClick,
                                onToggleWantToRead = onToggleWantToRead,
                                onToggleFinished = onToggleFinished,
                                onRenameBook = onRenameBook,
                                onDeleteBook = onDeleteBook,
                                onShareBook = onShareBook,
                                showAddToCollection = true,
                                onAddToCollection = onAddToCollection,
                                collections = collections,
                                viewMode = viewMode,
                                onViewModeChange = onViewModeChange,
                                sortOption = sortOption,
                                onSortOptionChange = onSortOptionChange,
                            )
                        }
                    }

                    1 -> {
                        when {
                            isLoading -> LoadingState()
                            audiobooks.isEmpty() -> Box(
                                modifier = Modifier.fillMaxSize(),
                                contentAlignment = Alignment.Center
                            ) {
                                EmptyState(
                                    title = "No audiobooks",
                                    subtitle = "Add a folder with mp3 files to listen",
                                    image = R.drawable.audiobooks_empty, // Consider using a different icon
                                    actionLabel = "Import Folder",
                                    onAction = onAddBooks, // Assume + is also used for folders
                                    modifier = Modifier.padding(horizontal = 24.dp),
                                )
                            }

                            else -> AudiobookGrid(
                                audiobooks = audiobooks,
                                onBookClick = onBookClick,
                                onDeleteBook = onDeleteBook,
                            )
                        }
                    }

                    2 -> {
                        CollectionsListScreen(
                            collections = collections,
                            onCollectionClick = { onCollectionClick(it.id) },
                            onCreateCollection = onCreateCollection,
                        )
                    }
                }
            }
        }

        // Header overlay — slides up and fades out as the user scrolls down.
        // Uses graphicsLayer so the animation runs at draw time, not composition time.
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .onSizeChanged { headerHeightState.intValue = it.height }
                .graphicsLayer {
                    val scrolled = scrolledPxState.floatValue
                    val maxScroll = headerHeightState.intValue.toFloat().coerceAtLeast(1f)
                    translationY = -scrolled
                    alpha = (1f - scrolled / maxScroll).coerceIn(0f, 1f)
                }
        ) {
            LiberHeader(
                title = "Library"
            )
        }

        if (selectedCollectionId != null && selectedCollection != null) {
            Surface(
                modifier = Modifier.fillMaxSize(),
                color = MaterialTheme.colorScheme.background
            ) {
                CollectionDetailScreen(
                    collection = selectedCollection,
                    allBooks = books,
                    onBack = { onCollectionClick(null) },
                    onRename = { onRenameCollection(selectedCollection.id, it) },
                    onDelete = {
                        onDeleteCollection(selectedCollection.id)
                        onCollectionClick(null)
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
                    viewMode = viewMode,
                    onViewModeChange = onViewModeChange,
                    sortOption = sortOption,
                    onSortOptionChange = onSortOptionChange,
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
    selectedCollectionId: Long? = null,
    onCollectionClick: (Long?) -> Unit = {},
    modifier: Modifier = Modifier,
) {
    val books by viewModel.books.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()
    val collections by collectionsViewModel.collections.collectAsState()
    val viewMode by viewModel.libraryViewMode.collectAsState()
    val sortOption by viewModel.librarySortOption.collectAsState()
    val scanState by viewModel.scanState.collectAsState()

    LibraryScreen(
        books = books,
        isLoading = isLoading,
        onBookClick = onBookClick,
        onAddBooks = onAddBooks,
        scanState = scanState,
        onDismissScanBanner = { viewModel.dismissScanBanner() },
        onToggleWantToRead = { book -> viewModel.toggleWantToRead(book.id, book.wantToRead) },
        onToggleFinished = { book ->
            viewModel.toggleFinished(
                book.id,
                book.readingProgress == 100
            )
        },
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
            collectionsViewModel.addBookToCollection(id, bookId)
        },
        onRemoveBookFromCollection = { id, bookId ->
            collectionsViewModel.removeBookFromCollection(id, bookId)
        },
        viewMode = viewMode,
        onViewModeChange = { viewModel.setLibraryViewMode(it) },
        sortOption = sortOption,
        onSortOptionChange = { viewModel.setLibrarySortOption(it) },
        selectedCollectionId = selectedCollectionId,
        onCollectionClick = onCollectionClick,
        modifier = modifier,
    )
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
