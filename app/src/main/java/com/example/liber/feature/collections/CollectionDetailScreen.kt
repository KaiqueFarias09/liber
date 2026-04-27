package com.example.liber.feature.collections

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.adamglin.PhosphorIcons
import com.adamglin.phosphoricons.Regular
import com.adamglin.phosphoricons.regular.DotsThreeVertical
import com.adamglin.phosphoricons.regular.PencilSimple
import com.adamglin.phosphoricons.regular.Plus
import com.adamglin.phosphoricons.regular.Trash
import com.example.liber.R
import com.example.liber.core.designsystem.AppErrorState
import com.example.liber.core.designsystem.BookGrid
import com.example.liber.core.designsystem.EmptyState
import com.example.liber.core.designsystem.LiberContextMenuDivider
import com.example.liber.core.designsystem.LiberContextMenuItem
import com.example.liber.core.designsystem.LiberDropdownMenu
import com.example.liber.core.designsystem.LiberLibraryToolbar
import com.example.liber.core.designsystem.LiberScreen
import com.example.liber.core.util.UiState
import com.example.liber.core.util.UiText
import com.example.liber.data.model.Book
import com.example.liber.data.model.BookPreview
import com.example.liber.feature.collections.components.AddBooksDialog
import com.example.liber.feature.collections.components.CollectionNameDialog
import com.example.liber.feature.collections.components.DeleteCollectionDialog
import com.example.liber.feature.library.LibrarySortOption
import com.example.liber.feature.library.LibraryViewMode

@Composable
fun CollectionDetailRoute(
    viewModel: CollectionDetailViewModel,
    onBack: () -> Unit,
    onOpenBook: (BookPreview) -> Unit,
    onShareBook: (BookPreview) -> Unit,
    onToggleWantToRead: (BookPreview) -> Unit,
    onToggleFinished: (BookPreview) -> Unit,
    homeViewModel: com.example.liber.feature.home.HomeViewModel,
    activeBookId: String? = null,
    isPlaying: Boolean = false,
) {
    val collectionState by viewModel.collectionState.collectAsState()
    val allBooks by viewModel.allBooks.collectAsState()
    var selectedBookForDetails by remember { mutableStateOf<BookPreview?>(null) }
    var fullBookDetail by remember { mutableStateOf<Book?>(null) }

    LaunchedEffect(selectedBookForDetails) {
        selectedBookForDetails?.let { preview ->
            fullBookDetail = homeViewModel.bookRepository.getBookById(preview.id)
        }
    }

    var viewMode by remember { mutableStateOf(LibraryViewMode.GRID) }
    var sortOption by remember { mutableStateOf(LibrarySortOption.RECENT) }

    CollectionDetailScreen(
        collectionState = collectionState,
        allBooks = allBooks,
        onBack = onBack,
        onRename = viewModel::renameCollection,
        onDelete = {
            viewModel.deleteCollection()
            onBack()
        },
        onAddBook = viewModel::addBookToCollection,
        onRemoveBook = viewModel::removeBookFromCollection,
        onOpenBook = onOpenBook,
        onShareBook = onShareBook,
        onToggleWantToRead = onToggleWantToRead,
        onToggleFinished = onToggleFinished,
        onShowDetails = { selectedBookForDetails = it },
        viewMode = viewMode,
        onViewModeChange = { viewMode = it },
        sortOption = sortOption,
        onSortOptionChange = { sortOption = it },
        activeAudiobookId = activeBookId,
        isAudiobookPlaying = isPlaying,
    )

    fullBookDetail?.let { book ->
        com.example.liber.feature.home.components.BookDetailsBottomSheet(
            book = book,
            homeViewModel = homeViewModel,
            onDismiss = {
                selectedBookForDetails = null
                fullBookDetail = null
            },
            onDelete = {
                homeViewModel.deleteBook(book.id)
                selectedBookForDetails = null
                fullBookDetail = null
            },
            onShare = { onShareBook(selectedBookForDetails!!) },
        )
    }
}

@Composable
fun CollectionDetailScreen(
    collectionState: UiState<CollectionDetailUiState>,
    allBooks: List<BookPreview>,
    onBack: () -> Unit,
    onRename: (String) -> Unit,
    onDelete: () -> Unit,
    onAddBook: (String) -> Unit,
    onRemoveBook: (String) -> Unit,
    onOpenBook: (BookPreview) -> Unit,
    onShareBook: (BookPreview) -> Unit,
    onToggleWantToRead: (BookPreview) -> Unit,
    onToggleFinished: (BookPreview) -> Unit,
    onShowDetails: (BookPreview) -> Unit,
    viewMode: LibraryViewMode = LibraryViewMode.GRID,
    onViewModeChange: (LibraryViewMode) -> Unit = {},
    sortOption: LibrarySortOption = LibrarySortOption.RECENT,
    onSortOptionChange: (LibrarySortOption) -> Unit = {},
    activeAudiobookId: String? = null,
    isAudiobookPlaying: Boolean = false,
) {
    when (collectionState) {
        is UiState.Loading -> {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
        }

        is UiState.Error -> {
            AppErrorState(
                title = collectionState.title,
                message = collectionState.message,
            )
        }

        is UiState.Success -> {
            val collection = collectionState.data
            CollectionDetailContent(
                collection = collection,
                allBooks = allBooks,
                onBack = onBack,
                onRename = onRename,
                onDelete = onDelete,
                onAddBook = onAddBook,
                onRemoveBook = onRemoveBook,
                onOpenBook = onOpenBook,
                onShareBook = onShareBook,
                onToggleWantToRead = onToggleWantToRead,
                onToggleFinished = onToggleFinished,
                onShowDetails = onShowDetails,
                viewMode = viewMode,
                onViewModeChange = onViewModeChange,
                sortOption = sortOption,
                onSortOptionChange = onSortOptionChange,
                activeAudiobookId = activeAudiobookId,
                isAudiobookPlaying = isAudiobookPlaying,
            )
        }
    }
}

@Composable
private fun CollectionDetailContent(
    collection: CollectionDetailUiState,
    allBooks: List<BookPreview>,
    onBack: () -> Unit,
    onRename: (String) -> Unit,
    onDelete: () -> Unit,
    onAddBook: (String) -> Unit,
    onRemoveBook: (String) -> Unit,
    onOpenBook: (BookPreview) -> Unit,
    onShareBook: (BookPreview) -> Unit,
    onToggleWantToRead: (BookPreview) -> Unit,
    onToggleFinished: (BookPreview) -> Unit,
    onShowDetails: (BookPreview) -> Unit,
    viewMode: LibraryViewMode,
    onViewModeChange: (LibraryViewMode) -> Unit,
    sortOption: LibrarySortOption,
    onSortOptionChange: (LibrarySortOption) -> Unit,
    activeAudiobookId: String?,
    isAudiobookPlaying: Boolean,
) {
    var showMenu by remember { mutableStateOf(false) }
    var showRenameDialog by remember { mutableStateOf(false) }
    var showDeleteDialog by remember { mutableStateOf(false) }
    var showAddBooksSheet by remember { mutableStateOf(false) }

    LiberScreen(
        title = UiText.DynamicString(collection.name),
        onBack = onBack,
        headerActions = {
            if (!collection.isSmart) {
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
                            label = UiText.StringResource(R.string.action_add_books),
                            icon = PhosphorIcons.Regular.Plus,
                            onClick = { showMenu = false; showAddBooksSheet = true },
                        )
                        LiberContextMenuItem(
                            label = UiText.StringResource(R.string.action_rename),
                            icon = PhosphorIcons.Regular.PencilSimple,
                            onClick = { showMenu = false; showRenameDialog = true },
                        )
                        LiberContextMenuDivider()
                        LiberContextMenuItem(
                            label = UiText.StringResource(R.string.action_delete_collection),
                            icon = PhosphorIcons.Regular.Trash,
                            destructive = true,
                            onClick = { showMenu = false; showDeleteDialog = true },
                        )
                    }
                }
            }
        }
    ) {
        if (collection.books.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp),
                contentAlignment = Alignment.Center,
            ) {
                EmptyState(
                    title = if (collection.isSmart) UiText.StringResource(R.string.error_no_results)
                    else UiText.StringResource(R.string.empty_collection_detail_title),
                    subtitle = if (collection.isSmart) UiText.DynamicString("No books match this smart collection's criteria.")
                    else UiText.StringResource(R.string.empty_collection_detail_subtitle),
                    image = R.drawable.collections_empty,
                    actionLabel = if (collection.isSmart) null else UiText.StringResource(R.string.empty_collection_detail_action),
                    onAction = { if (!collection.isSmart) showAddBooksSheet = true },
                )
            }
        } else {
            LiberLibraryToolbar(
                countText = androidx.compose.ui.res.pluralStringResource(
                    R.plurals.label_books,
                    collection.books.size,
                    collection.books.size
                ),
                sortOption = sortOption,
                onSortChange = onSortOptionChange,
                viewMode = viewMode,
                onViewModeChange = onViewModeChange,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp, vertical = 4.dp),
            )
            BookGrid(
                books = collection.books,
                onBookClick = onOpenBook,
                onToggleWantToRead = onToggleWantToRead,
                onToggleFinished = onToggleFinished,
                onShowDetails = onShowDetails,
                onDeleteBook = { if (!collection.isSmart) onRemoveBook(it.id) },
                onShareBook = onShareBook,
                modifier = Modifier.weight(1f),
                contentPadding = PaddingValues(horizontal = 24.dp, vertical = 8.dp),
                deleteLabel = if (collection.isSmart) null else UiText.StringResource(R.string.action_remove_from_collection),
                confirmDelete = false,
                showAddToCollection = true, // Still allow adding to other collections
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
            title = UiText.StringResource(R.string.dialog_title_rename_collection),
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
