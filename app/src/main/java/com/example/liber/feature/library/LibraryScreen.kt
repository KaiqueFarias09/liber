package com.example.liber.feature.library

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
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.nestedscroll.NestedScrollConnection
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp
import com.example.liber.R
import com.example.liber.core.designsystem.BookGrid
import com.example.liber.core.designsystem.EmptyState
import com.example.liber.core.designsystem.LiberHeader
import com.example.liber.core.designsystem.LiberTabBar
import com.example.liber.core.designsystem.ScanProgressBanner
import com.example.liber.core.util.UiState
import com.example.liber.core.util.UiText
import com.example.liber.data.model.Book
import com.example.liber.data.model.ScanState
import com.example.liber.feature.audiobook.components.AudiobookGrid
import com.example.liber.feature.collections.CollectionDetailScreen
import com.example.liber.feature.collections.CollectionUiState
import com.example.liber.feature.collections.CollectionsListScreen
import com.example.liber.feature.collections.CollectionsViewModel
import com.example.liber.feature.home.HomeViewModel
import com.example.liber.feature.home.components.BookDetailsBottomSheet
import kotlinx.coroutines.launch

@Composable
fun LibraryScreen(
    booksState: UiState<List<Book>>,
    onBookClick: (Book) -> Unit,
    onAddBooks: () -> Unit,
    onToggleWantToRead: (Book) -> Unit,
    onToggleFinished: (Book) -> Unit,
    onShowDetails: (Book) -> Unit,
    onDeleteBook: (Book) -> Unit,
    onShareBook: (Book) -> Unit,
    modifier: Modifier = Modifier,
    scanState: ScanState = ScanState.Idle,
    onDismissScanBanner: () -> Unit = {},
    onAddToCollection: (Book, Long) -> Unit = { _, _ -> },
    collectionsState: UiState<List<CollectionUiState>> = UiState.Loading,
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
    selectedTabIndex: Int = 0,
    onTabSelected: (Int) -> Unit = {},
    activeBookId: String? = null,
    isPlaying: Boolean = false,
) {
    val pagerState = rememberPagerState(initialPage = selectedTabIndex) { 3 }
    val scope = rememberCoroutineScope()

    val books = (booksState as? UiState.Success)?.data ?: emptyList()
    val collections = (collectionsState as? UiState.Success)?.data ?: emptyList()
    val selectedCollection = collections.find { it.id == selectedCollectionId }

    val currentTabIndex = pagerState.currentPage

    LaunchedEffect(currentTabIndex) {
        onTabSelected(currentTabIndex)
        if (currentTabIndex != 2) {
            onCollectionClick(null)
        }
    }

    val density = LocalDensity.current
    val headerHeightState = remember { mutableIntStateOf(0) }
    val scrolledPxState = remember { mutableFloatStateOf(0f) }

    val nestedScrollConnection = remember {
        object : NestedScrollConnection {
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
                tabs = listOf(
                    UiText.StringResource(R.string.tab_books),
                    UiText.StringResource(R.string.tab_audiobooks),
                    UiText.StringResource(R.string.tab_collections)
                ),
                selectedTabIndex = currentTabIndex,
                onTabSelected = { index ->
                    scope.launch {
                        pagerState.animateScrollToPage(index)
                    }
                }
            )

            Spacer(Modifier.height(8.dp))

            HorizontalPager(
                state = pagerState,
                modifier = Modifier.weight(1f),
                verticalAlignment = Alignment.Top,
            ) { page ->
                when (page) {
                    0 -> {
                        LibraryBooksTab(
                            booksState = booksState,
                            isAudiobook = false,
                            onBookClick = onBookClick,
                            onAddBooks = onAddBooks,
                            onToggleWantToRead = onToggleWantToRead,
                            onToggleFinished = onToggleFinished,
                            onShowDetails = onShowDetails,
                            onDeleteBook = onDeleteBook,
                            onShareBook = onShareBook,
                            onAddToCollection = onAddToCollection,
                            collections = collections,
                            viewMode = viewMode,
                            onViewModeChange = onViewModeChange,
                            sortOption = sortOption,
                            onSortOptionChange = onSortOptionChange,
                            activeBookId = activeBookId,
                            isPlaying = isPlaying
                        )
                    }

                    1 -> {
                        LibraryBooksTab(
                            booksState = booksState,
                            isAudiobook = true,
                            onBookClick = onBookClick,
                            onAddBooks = onAddBooks,
                            onToggleWantToRead = onToggleWantToRead,
                            onToggleFinished = onToggleFinished,
                            onShowDetails = onShowDetails,
                            onDeleteBook = onDeleteBook,
                            onShareBook = onShareBook,
                            onAddToCollection = onAddToCollection,
                            collections = collections,
                            viewMode = viewMode,
                            onViewModeChange = onViewModeChange,
                            sortOption = sortOption,
                            onSortOptionChange = onSortOptionChange,
                            activeBookId = activeBookId,
                            isPlaying = isPlaying
                        )
                    }

                    2 -> {
                        when (collectionsState) {
                            is UiState.Loading -> LoadingState()
                            is UiState.Error -> ErrorState(collectionsState.message)
                            is UiState.Success -> {
                                CollectionsListScreen(
                                    collections = collectionsState.data,
                                    onCollectionClick = { onCollectionClick(it.id) },
                                    onCreateCollection = onCreateCollection,
                                )
                            }
                        }
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
                title = UiText.DynamicString("Library")
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
                    onShowDetails = onShowDetails,
                    viewMode = viewMode,
                    onViewModeChange = onViewModeChange,
                    sortOption = sortOption,
                    onSortOptionChange = onSortOptionChange,
                    activeAudiobookId = activeBookId,
                    isAudiobookPlaying = isPlaying,
                )
            }
        }
    }
}

@Composable
private fun LibraryBooksTab(
    booksState: UiState<List<Book>>,
    isAudiobook: Boolean,
    onBookClick: (Book) -> Unit,
    onAddBooks: () -> Unit,
    onToggleWantToRead: (Book) -> Unit,
    onToggleFinished: (Book) -> Unit,
    onShowDetails: (Book) -> Unit,
    onDeleteBook: (Book) -> Unit,
    onShareBook: (Book) -> Unit,
    onAddToCollection: (Book, Long) -> Unit,
    collections: List<CollectionUiState>,
    viewMode: LibraryViewMode,
    onViewModeChange: (LibraryViewMode) -> Unit,
    sortOption: LibrarySortOption,
    onSortOptionChange: (LibrarySortOption) -> Unit,
    activeBookId: String?,
    isPlaying: Boolean,
) {
    when (booksState) {
        is UiState.Loading -> LoadingState()
        is UiState.Error -> ErrorState(booksState.message)
        is UiState.Success -> {
            val books = booksState.data.filter { it.isAudiobook == isAudiobook }
            if (books.isEmpty()) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
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
                    )
                }
            }
        }
    }
}

@Composable
private fun LoadingState() {
    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        CircularProgressIndicator()
    }
}

@Composable
private fun ErrorState(message: UiText) {
    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Text(text = message.asString(), color = MaterialTheme.colorScheme.error)
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
    liberAppViewModel: com.example.liber.ui.LiberAppViewModel,
    onCollectionClick: (Long?) -> Unit,
    modifier: Modifier = Modifier,
    selectedCollectionId: Long? = null,
) {
    val booksState by viewModel.booksState.collectAsState()
    val collectionsState by collectionsViewModel.collectionsState.collectAsState()
    val viewMode by viewModel.libraryViewMode.collectAsState()
    val sortOption by viewModel.librarySortOption.collectAsState()
    val scanState by viewModel.scanState.collectAsState()

    val activeBook by liberAppViewModel.activeBook.collectAsState()
    val isPlaying by liberAppViewModel.isPlaying.collectAsState()
    val selectedTabIndex by liberAppViewModel.libraryTabIndex.collectAsState()

    var selectedBookForDetails by remember { mutableStateOf<Book?>(null) }

    LibraryScreen(
        booksState = booksState,
        onBookClick = onBookClick,
        onAddBooks = onAddBooks,
        onToggleWantToRead = { book -> viewModel.toggleWantToRead(book.id, book.wantToRead) },
        onToggleFinished = { book ->
            viewModel.toggleFinished(book.id, book.readingProgress == 100)
        },
        onShowDetails = { selectedBookForDetails = it },
        onDeleteBook = { book -> viewModel.deleteBook(book.id) },
        onShareBook = { book -> onShareBook(book) },
        onAddToCollection = { book, collectionId ->
            collectionsViewModel.addBookToCollection(collectionId, book.id)
        },
        collectionsState = collectionsState,
        onCreateCollection = { collectionsViewModel.createCollection(it) },
        onRenameCollection = { id, name -> collectionsViewModel.renameCollection(id, name) },
        onDeleteCollection = { id -> collectionsViewModel.deleteCollection(id) },
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
        viewMode = viewMode,
        onViewModeChange = { viewModel.setLibraryViewMode(it) },
        sortOption = sortOption,
        onSortOptionChange = { viewModel.setLibrarySortOption(it) },
        modifier = modifier,
        scanState = scanState,
        onDismissScanBanner = { viewModel.dismissScanBanner() },
        selectedCollectionId = selectedCollectionId,
        onCollectionClick = onCollectionClick,
        selectedTabIndex = selectedTabIndex,
        onTabSelected = { liberAppViewModel.setLibraryTabIndex(it) },
        activeBookId = activeBook?.id,
        isPlaying = isPlaying,
    )

    selectedBookForDetails?.let { book ->
        BookDetailsBottomSheet(
            book = book,
            homeViewModel = viewModel,
            onDismiss = { selectedBookForDetails = null },
            onDelete = { viewModel.deleteBook(book.id) },
            onShare = { onShareBook(book) },
        )
    }
}
