package com.example.liber.feature.library

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.adamglin.PhosphorIcons
import com.adamglin.phosphoricons.Regular
import com.adamglin.phosphoricons.regular.MagnifyingGlass
import com.adamglin.phosphoricons.regular.Rows
import com.adamglin.phosphoricons.regular.SquaresFour
import com.example.liber.R
import com.example.liber.core.designsystem.AppErrorState
import com.example.liber.core.designsystem.BookGrid
import com.example.liber.core.designsystem.EditorialDropdown
import com.example.liber.core.designsystem.EmptyState
import com.example.liber.core.designsystem.Gambetta
import com.example.liber.core.designsystem.LiberCollapsingScreen
import com.example.liber.core.designsystem.LiberTabBar
import com.example.liber.core.designsystem.ScanProgressBanner
import com.example.liber.core.util.UiState
import com.example.liber.core.util.UiText
import com.example.liber.data.local.DictionaryEntryWithSenses
import com.example.liber.data.model.Book
import com.example.liber.data.model.BookPreview
import com.example.liber.data.model.ScanState
import com.example.liber.feature.audiobook.components.AudiobookGrid
import com.example.liber.feature.collections.CollectionUiState
import com.example.liber.feature.collections.CollectionsListScreen
import com.example.liber.feature.collections.CollectionsViewModel
import com.example.liber.feature.dictionary.DictionaryViewModel
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
    searchQuery: String = "",
    onSearchQueryChange: (String) -> Unit = {},
    onSearch: (String) -> Unit = {},
    filterStatus: LibraryFilterStatus = LibraryFilterStatus.ALL,
    onFilterStatusChange: (LibraryFilterStatus) -> Unit = {},
    autoCollectionsEnabled: Boolean = true,
    onAutoCollectionsToggle: (Boolean) -> Unit = {},
    onOpenDictionaryManager: () -> Unit = {},
    isSearchOpen: Boolean = false,
    onSearchOpenChange: (Boolean) -> Unit = {},
    selectedTabIndex: Int = 0,
    onTabSelected: (Int) -> Unit = {},
    activeBookId: String? = null,
    isPlaying: Boolean = false,
    recentSearches: List<String> = emptyList(),
    hasDictionaries: Boolean = false,
    searchType: SearchType = SearchType.ALL,
    onSearchTypeChange: (SearchType) -> Unit = {},
    dictionaryResults: UiState<List<DictionaryEntryWithSenses>> = UiState.Success(emptyList()),
) {
    val pagerState = rememberPagerState(initialPage = selectedTabIndex) { 3 }
    val scope = rememberCoroutineScope()

    BackHandler(enabled = isSearchOpen) {
        onSearchOpenChange(false)
    }

    val collections = (collectionsState as? UiState.Success)?.data ?: emptyList()

    val currentTabIndex = pagerState.currentPage

    LaunchedEffect(currentTabIndex) {
        onTabSelected(currentTabIndex)
    }

    Box(modifier = modifier.fillMaxSize()) {
        LiberCollapsingScreen(
            title = UiText.StringResource(R.string.tab_library),
            headerActions = {
                Surface(
                    onClick = { onSearchOpenChange(true) },
                    color = MaterialTheme.colorScheme.surface,
                    shape = CircleShape,
                    border = androidx.compose.foundation.BorderStroke(
                        1.dp,
                        MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f),
                    ),
                    modifier = Modifier.size(40.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = PhosphorIcons.Regular.MagnifyingGlass,
                            contentDescription = "Search",
                            modifier = Modifier.size(18.dp),
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
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
                },
                modifier = Modifier.padding(top = 8.dp)
            )

            Spacer(Modifier.height(8.dp))

            LibraryFilterAndSortRow(
                currentTabIndex = currentTabIndex,
                filterStatus = filterStatus,
                onFilterStatusChange = onFilterStatusChange,
                booksSortOption = booksSortOption,
                onBooksSortOptionChange = onBooksSortOptionChange,
                booksViewMode = booksViewMode,
                onBooksViewModeChange = onBooksViewModeChange,
                audiobooksSortOption = audiobooksSortOption,
                onAudiobooksSortOptionChange = onAudiobooksSortOptionChange,
                audiobooksViewMode = audiobooksViewMode,
                onAudiobooksViewModeChange = onAudiobooksViewModeChange,
                autoCollectionsEnabled = autoCollectionsEnabled,
                onAutoCollectionsToggle = onAutoCollectionsToggle
            )

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
                            searchQuery = searchQuery,
                            onClearSearch = { onSearchQueryChange("") },
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
                            searchQuery = searchQuery,
                            onClearSearch = { onSearchQueryChange("") },
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

        LibrarySearchOverlay(
            isOpen = isSearchOpen,
            onClose = {
                onSearchOpenChange(false)
                onSearchQueryChange("")
                onSearchTypeChange(SearchType.ALL)
            },
            searchQuery = searchQuery,
            onSearchQueryChange = onSearchQueryChange,
            booksState = booksState,
            onBookClick = {
                onSearchOpenChange(false)
                onBookClick(it)
            },
            recentSearches = recentSearches,
            onSearch = onSearch,
            onTabSelected = onTabSelected,
            onOpenDictionaryManager = onOpenDictionaryManager,
            hasDictionaries = hasDictionaries,
            searchType = searchType,
            onSearchTypeChange = onSearchTypeChange,
            dictionaryResults = dictionaryResults
        )
    }
}

@Composable
private fun LibraryFilterAndSortRow(
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
            Column(modifier = Modifier.fillMaxSize()) {
                if (books.isNotEmpty()) {
                    Text(
                        text = "${books.size} ${if (isAudiobook) "audiobooks" else "books"}",
                        style = MaterialTheme.typography.labelSmall.copy(
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 1.5.sp,
                            fontSize = 9.sp
                        ),
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f),
                        modifier = Modifier.padding(horizontal = 24.dp, vertical = 8.dp)
                    )
                }

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
    dictionaryViewModel: DictionaryViewModel,
    liberAppViewModel: com.example.liber.ui.LiberAppViewModel,
    onCollectionClick: (Long?) -> Unit,
    modifier: Modifier = Modifier,
    onOpenDictionaryManager: () -> Unit = {},
) {
    val booksState by viewModel.booksState.collectAsState()
    val collectionsState by collectionsViewModel.collectionsState.collectAsState()
    val booksViewMode by viewModel.booksViewMode.collectAsState()
    val booksSortOption by viewModel.booksSortOption.collectAsState()
    val audiobooksViewMode by viewModel.audiobooksViewMode.collectAsState()
    val audiobooksSortOption by viewModel.audiobooksSortOption.collectAsState()
    val scanState by viewModel.scanState.collectAsState()
    val searchQuery by viewModel.librarySearchQuery.collectAsState()
    val filterStatus by viewModel.libraryFilterStatus.collectAsState()
    val isSearchOpen by viewModel.isLibrarySearchOpen.collectAsState()
    val autoCollectionsEnabled by collectionsViewModel.autoCollectionsEnabled.collectAsState()
    val recentSearches by viewModel.recentSearches.collectAsState()
    val searchType by viewModel.librarySearchType.collectAsState()

    val activeBook by liberAppViewModel.activeBook.collectAsState()
    val isPlaying by liberAppViewModel.isPlaying.collectAsState()
    val selectedTabIndex by liberAppViewModel.libraryTabIndex.collectAsState()
    val dictionaries by dictionaryViewModel.dictionaries.collectAsState()
    val dictionaryLookupState by dictionaryViewModel.lookupState.collectAsState()

    var selectedBookForDetails by remember { mutableStateOf<BookPreview?>(null) }
    var fullBookDetail by remember { mutableStateOf<Book?>(null) }

    LaunchedEffect(selectedBookForDetails) {
        fullBookDetail = selectedBookForDetails?.let { preview ->
            viewModel.bookRepository.getBookById(preview.id)
        }
    }

    LaunchedEffect(searchQuery, searchType) {
        if (searchType == SearchType.DICTIONARY && searchQuery.isNotBlank()) {
            dictionaryViewModel.lookupWord(searchQuery, "en", null)
        }
    }

    LibraryScreen(
        booksState = booksState,
        onAddBooks = onAddBooks,
        onToggleWantToRead = { book: BookPreview ->
            viewModel.toggleWantToRead(
                book.id,
                book.wantToRead
            )
        },
        onToggleFinished = { book: BookPreview ->
            viewModel.toggleFinished(book.id, book.readingProgress == 100)
        },
        onShowDetails = { book: BookPreview -> selectedBookForDetails = book },
        onDeleteBook = { book: BookPreview -> viewModel.deleteBook(book.id) },
        onShareBook = { book: BookPreview -> onShareBook(book) },
        onAddToCollection = { book: BookPreview, collectionId: Long ->
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
        isSearchOpen = isSearchOpen,
        onSearchOpenChange = { viewModel.setLibrarySearchOpen(it) },
        selectedTabIndex = selectedTabIndex,
        onTabSelected = { liberAppViewModel.setLibraryTabIndex(it) },
        activeBookId = activeBook?.id,
        isPlaying = isPlaying,
        searchQuery = searchQuery,
        onSearchQueryChange = { viewModel.setLibrarySearchQuery(it) },
        onSearch = { viewModel.addRecentSearch(it) },
        filterStatus = filterStatus,
        onFilterStatusChange = { viewModel.setLibraryFilterStatus(it) },
        autoCollectionsEnabled = autoCollectionsEnabled,
        onAutoCollectionsToggle = { collectionsViewModel.setAutoCollectionsEnabled(it) },
        onOpenDictionaryManager = onOpenDictionaryManager,
        recentSearches = recentSearches,
        hasDictionaries = dictionaries.isNotEmpty(),
        searchType = searchType,
        onSearchTypeChange = { viewModel.setLibrarySearchType(it) },
        dictionaryResults = dictionaryLookupState,
        onBookClick = { book ->
            if (searchQuery.isNotBlank()) {
                viewModel.addRecentSearch(searchQuery)
            }
            onBookClick(book)
        }
    )

    fullBookDetail?.let { book ->
        BookDetailsBottomSheet(
            book = book,
            homeViewModel = viewModel,
            onDismiss = {
                selectedBookForDetails = null
            },
            onDelete = {
                viewModel.deleteBook(book.id)
                selectedBookForDetails = null
            },
            onShare = { onShareBook(selectedBookForDetails!!) },
        )
    }
}
