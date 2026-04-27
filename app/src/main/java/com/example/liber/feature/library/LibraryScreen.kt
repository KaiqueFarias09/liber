package com.example.liber.feature.library

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.material3.CircularProgressIndicator
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
import androidx.compose.ui.unit.dp
import com.example.liber.R
import com.example.liber.core.designsystem.AppErrorState
import com.example.liber.core.designsystem.BookGrid
import com.example.liber.core.designsystem.EmptyState
import com.example.liber.core.designsystem.LiberCollapsingScreen
import com.example.liber.core.designsystem.LiberTabBar
import com.example.liber.core.designsystem.ScanProgressBanner
import com.example.liber.core.util.UiState
import com.example.liber.core.util.UiText
import com.example.liber.data.model.Book
import com.example.liber.data.model.BookPreview
import com.example.liber.data.model.ScanState
import com.example.liber.feature.audiobook.components.AudiobookGrid
import com.example.liber.feature.collections.CollectionUiState
import com.example.liber.feature.collections.CollectionsListScreen
import com.example.liber.feature.collections.CollectionsViewModel
import com.example.liber.feature.home.HomeViewModel
import com.example.liber.feature.home.components.BookDetailsBottomSheet
import kotlinx.coroutines.launch

@Composable
fun LibraryScreen(
    booksState: UiState<List<BookPreview>>,
    onBookClick: (BookPreview) -> Unit,
    onAddBooks: () -> Unit,
    onToggleWantToRead: (BookPreview) -> Unit,
    onToggleFinished: (BookPreview) -> Unit,
    onShowDetails: (BookPreview) -> Unit,
    onDeleteBook: (BookPreview) -> Unit,
    onShareBook: (BookPreview) -> Unit,
    modifier: Modifier = Modifier,
    scanState: ScanState = ScanState.Idle,
    onDismissScanBanner: () -> Unit = {},
    onAddToCollection: (BookPreview, Long) -> Unit = { _, _ -> },
    collectionsState: UiState<List<CollectionUiState>> = UiState.Loading,
    onCreateCollection: (String) -> Unit = {},
    onCollectionClick: (Long?) -> Unit = {},
    booksViewMode: LibraryViewMode = LibraryViewMode.GRID,
    onBooksViewModeChange: (LibraryViewMode) -> Unit = {},
    booksSortOption: LibrarySortOption = LibrarySortOption.RECENT,
    onBooksSortOptionChange: (LibrarySortOption) -> Unit = {},
    audiobooksViewMode: LibraryViewMode = LibraryViewMode.GRID,
    onAudiobooksViewModeChange: (LibraryViewMode) -> Unit = {},
    audiobooksSortOption: LibrarySortOption = LibrarySortOption.RECENT,
    onAudiobooksSortOptionChange: (LibrarySortOption) -> Unit = {},
    selectedTabIndex: Int = 0,
    onTabSelected: (Int) -> Unit = {},
    activeBookId: String? = null,
    isPlaying: Boolean = false,
) {
    val pagerState = rememberPagerState(initialPage = selectedTabIndex) { 3 }
    val scope = rememberCoroutineScope()

    val collections = (collectionsState as? UiState.Success)?.data ?: emptyList()

    val currentTabIndex = pagerState.currentPage

    LaunchedEffect(currentTabIndex) {
        onTabSelected(currentTabIndex)
    }

    LiberCollapsingScreen(
        title = UiText.StringResource(R.string.tab_library),
        modifier = modifier
    ) {
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
                        viewMode = booksViewMode,
                        onViewModeChange = onBooksViewModeChange,
                        sortOption = booksSortOption,
                        onSortOptionChange = onBooksSortOptionChange,
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
                        viewMode = audiobooksViewMode,
                        onViewModeChange = onAudiobooksViewModeChange,
                        sortOption = audiobooksSortOption,
                        onSortOptionChange = onAudiobooksSortOptionChange,
                        activeBookId = activeBookId,
                        isPlaying = isPlaying
                    )
                }

                2 -> {
                    when (collectionsState) {
                        is UiState.Loading -> LoadingState()
                        is UiState.Error -> AppErrorState(
                            title = collectionsState.title,
                            message = collectionsState.message,
                        )

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
}

@Composable
private fun LibraryBooksTab(
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
fun LibraryScreen(
    viewModel: HomeViewModel,
    onBookClick: (BookPreview) -> Unit,
    onAddBooks: () -> Unit,
    onShareBook: (BookPreview) -> Unit,
    collectionsViewModel: CollectionsViewModel,
    liberAppViewModel: com.example.liber.ui.LiberAppViewModel,
    onCollectionClick: (Long?) -> Unit,
    modifier: Modifier = Modifier,
) {
    val booksState by viewModel.booksState.collectAsState()
    val collectionsState by collectionsViewModel.collectionsState.collectAsState()
    val booksViewMode by viewModel.booksViewMode.collectAsState()
    val booksSortOption by viewModel.booksSortOption.collectAsState()
    val audiobooksViewMode by viewModel.audiobooksViewMode.collectAsState()
    val audiobooksSortOption by viewModel.audiobooksSortOption.collectAsState()
    val scanState by viewModel.scanState.collectAsState()

    val activeBook by liberAppViewModel.activeBook.collectAsState()
    val isPlaying by liberAppViewModel.isPlaying.collectAsState()
    val selectedTabIndex by liberAppViewModel.libraryTabIndex.collectAsState()

    var selectedBookForDetails by remember { mutableStateOf<BookPreview?>(null) }
    var fullBookDetail by remember { mutableStateOf<Book?>(null) }

    LaunchedEffect(selectedBookForDetails) {
        selectedBookForDetails?.let { preview ->
            fullBookDetail = viewModel.bookRepository.getBookById(preview.id)
        }
    }

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
        onCollectionClick = onCollectionClick,
        booksViewMode = booksViewMode,
        onBooksViewModeChange = { viewModel.setBooksViewMode(it) },
        booksSortOption = booksSortOption,
        onBooksSortOptionChange = { viewModel.setBooksSortOption(it) },
        audiobooksViewMode = audiobooksViewMode,
        onAudiobooksViewModeChange = { viewModel.setAudiobooksViewMode(it) },
        audiobooksSortOption = audiobooksSortOption,
        onAudiobooksSortOptionChange = { viewModel.setAudiobooksSortOption(it) },
        modifier = modifier,
        scanState = scanState,
        onDismissScanBanner = { viewModel.dismissScanBanner() },
        selectedTabIndex = selectedTabIndex,
        onTabSelected = { liberAppViewModel.setLibraryTabIndex(it) },
        activeBookId = activeBook?.id,
        isPlaying = isPlaying,
    )

    fullBookDetail?.let { book ->
        BookDetailsBottomSheet(
            book = book,
            homeViewModel = viewModel,
            onDismiss = {
                selectedBookForDetails = null
                fullBookDetail = null
            },
            onDelete = {
                viewModel.deleteBook(book.id)
                selectedBookForDetails = null
                fullBookDetail = null
            },
            onShare = { onShareBook(selectedBookForDetails!!) },
        )
    }
}
