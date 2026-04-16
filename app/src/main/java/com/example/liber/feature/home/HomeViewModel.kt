package com.example.liber.feature.home

import android.app.Application
import android.net.Uri
import androidx.documentfile.provider.DocumentFile
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.liber.core.util.UiState
import com.example.liber.data.local.ScanStateHolder
import com.example.liber.data.model.Annotation
import com.example.liber.data.model.Book
import com.example.liber.data.model.Bookmark
import com.example.liber.data.model.ScanSource
import com.example.liber.data.model.ScanState
import com.example.liber.data.repository.BookImporter
import com.example.liber.data.repository.BookRepository
import com.example.liber.data.repository.ScanSourceRepository
import com.example.liber.data.repository.UserPreferencesRepository
import com.example.liber.feature.library.LibrarySortOption
import com.example.liber.feature.library.LibraryViewMode
import com.example.liber.feature.reader.AnnotationRequest
import com.example.liber.ui.LiberAppViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import javax.inject.Inject

@HiltViewModel
class HomeViewModel @Inject constructor(
    application: Application,
    internal val bookRepository: BookRepository,
    private val scanSourceRepository: ScanSourceRepository,
    private val userPreferencesRepository: UserPreferencesRepository,
    val bookImporter: BookImporter,
) : AndroidViewModel(application) {

    private val sevenDaysMs = 7L * 24 * 60 * 60 * 1000

    // ── State flows ──────────────────────────────────────────────────────────

    val booksState: StateFlow<UiState<List<Book>>> = bookRepository.getAllBooks()
        .map { UiState.Success(it) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), UiState.Loading)

    // For compatibility with simple list consumers
    val books: StateFlow<List<Book>> = booksState
        .map { (it as? UiState.Success)?.data ?: emptyList() }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val continueReadingBooks: StateFlow<List<Book>> = bookRepository
        .getContinueReadingBooks(System.currentTimeMillis() - sevenDaysMs)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val wantToReadBooks: StateFlow<List<Book>> = bookRepository.getWantToReadBooks()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val previousBooks: StateFlow<List<Book>> = bookRepository
        .getPreviousBooks(System.currentTimeMillis() - sevenDaysMs)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading

    val scanState: StateFlow<ScanState> = ScanStateHolder.state
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), ScanState.Idle)

    val scanSources: StateFlow<List<ScanSource>> = scanSourceRepository.getAllSources()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val booksViewMode: StateFlow<LibraryViewMode> = userPreferencesRepository.booksViewMode
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), LibraryViewMode.GRID)

    val booksSortOption: StateFlow<LibrarySortOption> = userPreferencesRepository.booksSortOption
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), LibrarySortOption.RECENT)

    val audiobooksViewMode: StateFlow<LibraryViewMode> =
        userPreferencesRepository.audiobooksViewMode
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), LibraryViewMode.GRID)

    val audiobooksSortOption: StateFlow<LibrarySortOption> =
        userPreferencesRepository.audiobooksSortOption
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), LibrarySortOption.RECENT)

    fun setBooksViewMode(mode: LibraryViewMode) {
        viewModelScope.launch {
            userPreferencesRepository.setBooksViewMode(mode)
        }
    }

    fun setBooksSortOption(option: LibrarySortOption) {
        viewModelScope.launch {
            userPreferencesRepository.setBooksSortOption(option)
        }
    }

    fun setAudiobooksViewMode(mode: LibraryViewMode) {
        viewModelScope.launch {
            userPreferencesRepository.setAudiobooksViewMode(mode)
        }
    }

    fun setAudiobooksSortOption(option: LibrarySortOption) {
        viewModelScope.launch {
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
        } catch (_: Exception) { /* permission already held or not applicable */
        }
    }

    fun saveLocator(bookId: String, locatorJson: String, progress: Int) {
        viewModelScope.launch(Dispatchers.IO) {
            bookRepository.updateLastLocator(bookId, locatorJson, progress)
        }
    }

    fun toggleWantToRead(bookId: String, currentValue: Boolean) {
        viewModelScope.launch(Dispatchers.IO) {
            bookRepository.updateWantToRead(bookId, !currentValue)
        }
    }

    fun toggleFinished(bookId: String, isFinished: Boolean) {
        viewModelScope.launch(Dispatchers.IO) {
            val progress = if (isFinished) 0 else 100
            bookRepository.updateLastLocator(bookId, null, progress)
        }
    }

    fun markAsFinished(bookId: String) {
        viewModelScope.launch(Dispatchers.IO) {
            bookRepository.updateLastLocator(bookId, null, 100)
        }
    }

    fun updateMetadata(bookId: String, title: String, author: String?, narrator: String?) {
        viewModelScope.launch(Dispatchers.IO) {
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
        viewModelScope.launch(Dispatchers.IO) {
            bookRepository.updateFullMetadata(bookId, title, author, coverPath, narrator)
        }
    }

    fun updateCoverPath(bookId: String, coverPath: String?) {
        viewModelScope.launch(Dispatchers.IO) {
            bookRepository.updateCoverPath(bookId, coverPath)
        }
    }

    fun deleteBook(bookId: String) {
        viewModelScope.launch(Dispatchers.IO) {
            bookRepository.deleteBook(bookId)
        }
    }

    fun getAnnotationsForBook(bookId: String): Flow<List<Annotation>> =
        bookRepository.getAnnotationsForBook(bookId)

    fun saveAnnotation(annotation: Annotation) {
        viewModelScope.launch(Dispatchers.IO) { bookRepository.insertAnnotation(annotation) }
    }

    fun deleteAnnotation(annotationId: Long) {
        viewModelScope.launch(Dispatchers.IO) { bookRepository.deleteAnnotation(annotationId) }
    }

    fun getBookmarksForBook(bookId: String): Flow<List<Bookmark>> =
        bookRepository.getBookmarksForBook(bookId)

    fun saveBookmark(bookmark: Bookmark) {
        viewModelScope.launch(Dispatchers.IO) { bookRepository.insertBookmark(bookmark) }
    }

    fun deleteBookmark(bookmarkId: Long) {
        viewModelScope.launch(Dispatchers.IO) { bookRepository.deleteBookmark(bookmarkId) }
    }

    fun loadBooksFromUris(uris: List<Uri>) {
        viewModelScope.launch {
            _isLoading.value = true
            withContext(Dispatchers.IO) {
                uris.forEach { uri ->
                    importBook(uri)
                }
            }
            _isLoading.value = false
        }
    }

    fun importAndOpenBook(uri: Uri, liberAppViewModel: LiberAppViewModel) {
        viewModelScope.launch {
            _isLoading.value = true
            val book = withContext(Dispatchers.IO) { importBook(uri) }
            if (book != null) {
                openBook(book)
                liberAppViewModel.openEpub(book)
            }
            _isLoading.value = false
        }
    }

    private suspend fun importBook(uri: Uri): Book? {
        tryTakePersistablePermission(uri)
        val file = DocumentFile.fromSingleUri(getApplication(), uri) ?: return null
        val book = bookImporter.parseBook(file) ?: return null
        val existingBook = book.contentId
            ?.let { bookRepository.getBookByContentId(it) }
            ?: bookRepository.getBookByFileUri(uri.toString())

        if (existingBook == null) {
            bookRepository.insertBook(book)
        }
        return book
    }

    fun addScanSource(treeUri: Uri, folderName: String) {
        viewModelScope.launch(Dispatchers.IO) {
            scanSourceRepository.upsert(
                ScanSource(
                    treeUri = treeUri.toString(),
                    displayName = folderName,
                )
            )
        }
    }

    fun removeScanSource(treeUri: String) {
        viewModelScope.launch(Dispatchers.IO) {
            scanSourceRepository.delete(treeUri)
        }
    }

    fun dismissScanBanner() {
        ScanStateHolder.update(ScanState.Idle)
    }

    // ── Private helpers ──────────────────────────────────────────────────────

    private fun tryTakePersistablePermission(uri: Uri) {
        try {
            getApplication<Application>().contentResolver.takePersistableUriPermission(
                uri,
                android.content.Intent.FLAG_GRANT_READ_URI_PERMISSION
            )
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
}
