package com.example.liber.feature.home

import android.app.Application
import android.net.Uri
import androidx.documentfile.provider.DocumentFile
import androidx.lifecycle.viewModelScope
import com.example.liber.core.logging.AppLogger
import com.example.liber.core.logging.BaseAndroidViewModel
import com.example.liber.core.util.UiState
import com.example.liber.core.util.rethrowIfCancellation
import com.example.liber.data.local.ScanStateHolder
import com.example.liber.data.model.Annotation
import com.example.liber.data.model.Book
import com.example.liber.data.model.BookPreview
import com.example.liber.data.model.Bookmark
import com.example.liber.data.model.ScanSource
import com.example.liber.data.model.ScanState
import com.example.liber.data.repository.BookImporter
import com.example.liber.data.repository.BookRepository
import com.example.liber.data.repository.ScanSourceRepository
import com.example.liber.data.repository.UserPreferencesRepository
import com.example.liber.feature.library.LibraryFilterStatus
import com.example.liber.feature.library.LibrarySortOption
import com.example.liber.feature.library.LibraryViewMode
import com.example.liber.feature.reader.AnnotationRequest
import com.example.liber.service.BookImportService
import com.example.liber.ui.LiberAppViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.withContext
import javax.inject.Inject

@HiltViewModel
class HomeViewModel @Inject constructor(
    application: Application,
    internal val bookRepository: BookRepository,
    private val scanSourceRepository: ScanSourceRepository,
    private val userPreferencesRepository: UserPreferencesRepository,
    val bookImporter: BookImporter,
    private val appLogger: AppLogger,
) : BaseAndroidViewModel(application, "HomeViewModel", appLogger) {

    private val sevenDaysMs = 7L * 24 * 60 * 60 * 1000

    // ── State flows ──────────────────────────────────────────────────────────

    private val _librarySearchQuery = MutableStateFlow("")
    val librarySearchQuery: StateFlow<String> = _librarySearchQuery.asStateFlow()

    private val _libraryFilterStatus = MutableStateFlow(LibraryFilterStatus.ALL)
    val libraryFilterStatus: StateFlow<LibraryFilterStatus> = _libraryFilterStatus.asStateFlow()

    val booksViewMode: StateFlow<LibraryViewMode> = userPreferencesRepository.booksViewMode
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), LibraryViewMode.GRID)

    val booksSortOption: StateFlow<LibrarySortOption> = userPreferencesRepository.booksSortOption
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), LibrarySortOption.RECENT)

    val booksState: StateFlow<UiState<List<BookPreview>>> = combine(
        bookRepository.getAllBookPreviews(),
        librarySearchQuery,
        libraryFilterStatus,
        booksSortOption,
    ) { books, query, status, sort ->
        val filtered = books.filter { book ->
            val matchesSearch = if (query.isBlank()) true else {
                book.title.contains(query, ignoreCase = true) ||
                        (book.author?.contains(query, ignoreCase = true) ?: false)
            }
            val matchesStatus = when (status) {
                LibraryFilterStatus.ALL -> true
                LibraryFilterStatus.UNREAD -> book.readingProgress == 0
                LibraryFilterStatus.IN_PROGRESS -> book.readingProgress in 1..99
                LibraryFilterStatus.FINISHED -> book.readingProgress == 100
            }
            matchesSearch && matchesStatus
        }

        val sorted = when (sort) {
            LibrarySortOption.RECENT -> filtered.sortedByDescending { it.lastOpenedAt ?: 0L }
            LibrarySortOption.TITLE -> filtered.sortedBy { it.title.lowercase() }
            LibrarySortOption.AUTHOR -> filtered.sortedBy {
                it.author?.lowercase() ?: "zzzz"
            }

            LibrarySortOption.PROGRESS -> filtered.sortedByDescending { it.readingProgress }
        }

        UiState.Success(sorted)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), UiState.Loading)

    // For compatibility with simple list consumers
    val books: StateFlow<List<BookPreview>> = booksState
        .map { (it as? UiState.Success)?.data ?: emptyList() }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val continueReadingBooks: StateFlow<List<BookPreview>> = bookRepository
        .getContinueReadingBookPreviews(System.currentTimeMillis() - sevenDaysMs)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val wantToReadBooks: StateFlow<List<BookPreview>> = bookRepository.getWantToReadBookPreviews()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val previousBooks: StateFlow<List<BookPreview>> = bookRepository
        .getPreviousBookPreviews(System.currentTimeMillis() - sevenDaysMs)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading

    val scanState: StateFlow<ScanState> = ScanStateHolder.state
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), ScanState.Idle)

    val scanSources: StateFlow<List<ScanSource>> = scanSourceRepository.getAllSources()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val audiobooksViewMode: StateFlow<LibraryViewMode> =
        userPreferencesRepository.audiobooksViewMode
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), LibraryViewMode.GRID)

    val audiobooksSortOption: StateFlow<LibrarySortOption> =
        userPreferencesRepository.audiobooksSortOption
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), LibrarySortOption.RECENT)

    fun setBooksViewMode(mode: LibraryViewMode) {
        launchSafely(
            actionName = "setBooksViewMode",
            parameters = mapOf("mode" to mode.name),
        ) {
            userPreferencesRepository.setBooksViewMode(mode)
        }
    }

    fun setBooksSortOption(option: LibrarySortOption) {
        launchSafely(
            actionName = "setBooksSortOption",
            parameters = mapOf("option" to option.name),
        ) {
            userPreferencesRepository.setBooksSortOption(option)
        }
    }

    fun setLibrarySearchQuery(query: String) {
        _librarySearchQuery.value = query
    }

    fun setLibraryFilterStatus(status: LibraryFilterStatus) {
        _libraryFilterStatus.value = status
    }

    fun setAudiobooksViewMode(mode: LibraryViewMode) {
        launchSafely(
            actionName = "setAudiobooksViewMode",
            parameters = mapOf("mode" to mode.name),
        ) {
            userPreferencesRepository.setAudiobooksViewMode(mode)
        }
    }

    fun setAudiobooksSortOption(option: LibrarySortOption) {
        launchSafely(
            actionName = "setAudiobooksSortOption",
            parameters = mapOf("option" to option.name),
        ) {
            userPreferencesRepository.setAudiobooksSortOption(option)
        }
    }

    private val _pendingAnnotationRequest = MutableStateFlow<AnnotationRequest?>(null)
    val pendingAnnotationRequest: StateFlow<AnnotationRequest?> = _pendingAnnotationRequest

    fun requestAnnotation(request: AnnotationRequest) {
        _pendingAnnotationRequest.value = request
    }

    fun clearPendingAnnotation() {
        _pendingAnnotationRequest.value = null
    }

    // ── Actions ──────────────────────────────────────────────────────────────

    /** Records a book as last opened and ensures URI permission is held. */
    suspend fun openBook(book: Book) = withContext(Dispatchers.IO) {
        bookRepository.updateLastOpenedAt(book.id, System.currentTimeMillis())
        try {
            getApplication<Application>().contentResolver.takePersistableUriPermission(
                book.fileUri,
                android.content.Intent.FLAG_GRANT_READ_URI_PERMISSION
            )
        } catch (e: Exception) {
            e.rethrowIfCancellation()
            appLogger.warn(
                "Failed to persist read permission for ${book.fileUri}",
                tag = "HomeViewModel",
                throwable = e,
            )
        }
    }

    fun saveLocator(bookId: String, locatorJson: String, progress: Int) {
        launchSafely(
            actionName = "saveLocator",
            dispatcher = Dispatchers.IO,
            parameters = mapOf("bookId" to bookId, "progress" to progress),
        ) {
            bookRepository.updateLastLocator(bookId, locatorJson, progress)
        }
    }

    fun toggleWantToRead(bookId: String, currentValue: Boolean) {
        launchSafely(
            actionName = "toggleWantToRead",
            dispatcher = Dispatchers.IO,
            parameters = mapOf("bookId" to bookId, "currentValue" to currentValue),
        ) {
            bookRepository.updateWantToRead(bookId, !currentValue)
        }
    }

    fun toggleFinished(bookId: String, isFinished: Boolean) {
        launchSafely(
            actionName = "toggleFinished",
            dispatcher = Dispatchers.IO,
            parameters = mapOf("bookId" to bookId, "isFinished" to isFinished),
        ) {
            val progress = if (isFinished) 0 else 100
            bookRepository.updateLastLocator(bookId, null, progress)
        }
    }

    fun markAsFinished(bookId: String) {
        launchSafely(
            actionName = "markAsFinished",
            dispatcher = Dispatchers.IO,
            parameters = mapOf("bookId" to bookId),
        ) {
            bookRepository.updateLastLocator(bookId, null, 100)
        }
    }

    fun updateMetadata(bookId: String, title: String, author: String?, narrator: String?) {
        launchSafely(
            actionName = "updateMetadata",
            dispatcher = Dispatchers.IO,
            parameters = mapOf("bookId" to bookId, "title" to title),
        ) {
            bookRepository.updateMetadata(bookId, title, author, narrator)
        }
    }

    fun updateFullMetadata(
        bookId: String,
        title: String,
        author: String?,
        coverPath: String?,
        narrator: String?
    ) {
        launchSafely(
            actionName = "updateFullMetadata",
            dispatcher = Dispatchers.IO,
            parameters = mapOf(
                "bookId" to bookId,
                "title" to title,
                "hasCoverPath" to (coverPath != null)
            ),
        ) {
            bookRepository.updateFullMetadata(bookId, title, author, coverPath, narrator)
        }
    }

    fun updateCoverPath(bookId: String, coverPath: String?) {
        launchSafely(
            actionName = "updateCoverPath",
            dispatcher = Dispatchers.IO,
            parameters = mapOf("bookId" to bookId, "hasCoverPath" to (coverPath != null)),
        ) {
            bookRepository.updateCoverPath(bookId, coverPath)
        }
    }

    fun deleteBook(bookId: String) {
        launchSafely(
            actionName = "deleteBook",
            dispatcher = Dispatchers.IO,
            parameters = mapOf("bookId" to bookId),
        ) {
            bookRepository.deleteBook(bookId)
        }
    }

    fun getAnnotationsForBook(bookId: String): Flow<List<Annotation>> =
        bookRepository.getAnnotationsForBook(bookId)

    fun saveAnnotation(annotation: Annotation) {
        launchSafely(
            actionName = "saveAnnotation",
            dispatcher = Dispatchers.IO,
            parameters = mapOf("bookId" to annotation.bookId),
        ) { bookRepository.insertAnnotation(annotation) }
    }

    fun deleteAnnotation(annotationId: Long) {
        launchSafely(
            actionName = "deleteAnnotation",
            dispatcher = Dispatchers.IO,
            parameters = mapOf("annotationId" to annotationId),
        ) { bookRepository.deleteAnnotation(annotationId) }
    }

    fun getBookmarksForBook(bookId: String): Flow<List<Bookmark>> =
        bookRepository.getBookmarksForBook(bookId)

    fun saveBookmark(bookmark: Bookmark) {
        launchSafely(
            actionName = "saveBookmark",
            dispatcher = Dispatchers.IO,
            parameters = mapOf("bookId" to bookmark.bookId),
        ) { bookRepository.insertBookmark(bookmark) }
    }

    fun deleteBookmark(bookmarkId: Long) {
        launchSafely(
            actionName = "deleteBookmark",
            dispatcher = Dispatchers.IO,
            parameters = mapOf("bookmarkId" to bookmarkId),
        ) { bookRepository.deleteBookmark(bookmarkId) }
    }

    fun loadBooksFromUris(uris: List<Uri>) {
        BookImportService.startImport(getApplication(), uris)
    }

    fun importAndOpenBook(uri: Uri, liberAppViewModel: LiberAppViewModel) {
        launchSafely(
            actionName = "importAndOpenBook",
            parameters = mapOf("uri" to uri),
        ) {
            _isLoading.value = true
            try {
                val book = withContext(Dispatchers.IO) { importBook(uri) }
                if (book != null) {
                    openBook(book)
                    liberAppViewModel.openEpub(book)
                }
            } catch (e: Exception) {
                e.rethrowIfCancellation()
                logger.recordError(e, "importAndOpenBook", "Failed to import and open book")
            } finally {
                _isLoading.value = false
            }
        }
    }

    private suspend fun importBook(uri: Uri): Book? {
        tryTakePersistablePermission(uri)
        return runCatching {
            val file = DocumentFile.fromSingleUri(getApplication(), uri) ?: return null
            val book = bookImporter.parseBook(file) ?: return null
            val existingBook = book.contentId
                ?.let { bookRepository.getBookByContentId(it) }
                ?: bookRepository.getBookByFileUri(uri.toString())

            if (existingBook == null) {
                bookRepository.insertBook(book)
            }
            book
        }.onFailure { throwable ->
            throwable.rethrowIfCancellation()
            logger.recordError(throwable, "importBook", "Failed to import book uri=$uri")
        }.getOrNull()
    }

    private fun tryTakePersistablePermission(uri: Uri) {
        try {
            getApplication<Application>().contentResolver.takePersistableUriPermission(
                uri,
                android.content.Intent.FLAG_GRANT_READ_URI_PERMISSION
            )
        } catch (e: Exception) {
            e.rethrowIfCancellation()
            logger.recordError(
                e,
                "tryTakePersistablePermission",
                "Unable to persist read permission for uri=$uri"
            )
        }
    }

    fun addScanSource(treeUri: Uri, folderName: String) {
        launchSafely(
            actionName = "addScanSource",
            dispatcher = Dispatchers.IO,
            parameters = mapOf("treeUri" to treeUri, "folderName" to folderName),
        ) {
            scanSourceRepository.upsert(
                ScanSource(
                    treeUri = treeUri.toString(),
                    displayName = folderName,
                )
            )
        }
    }

    fun removeScanSource(treeUri: String) {
        launchSafely(
            actionName = "removeScanSource",
            dispatcher = Dispatchers.IO,
            parameters = mapOf("treeUri" to treeUri),
        ) {
            scanSourceRepository.delete(treeUri)
        }
    }

    fun dismissScanBanner() {
        ScanStateHolder.update(ScanState.Idle)
    }

    // ── Private helpers ──────────────────────────────────────────────────────
}
