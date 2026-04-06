package com.example.liber.ui.home

import android.app.Application
import android.net.Uri
import androidx.documentfile.provider.DocumentFile
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.liber.data.AnnotationEntity
import com.example.liber.data.AppDatabase
import com.example.liber.data.Book
import com.example.liber.data.BookImporter
import com.example.liber.data.BookRepository
import com.example.liber.data.BookmarkEntity
import com.example.liber.data.ScanSourceEntity
import com.example.liber.data.ScanSourceRepository
import com.example.liber.data.ScanState
import com.example.liber.data.ScanStateHolder
import com.example.liber.data.toEntity
import com.example.liber.ui.library.LibrarySortOption
import com.example.liber.ui.library.LibraryViewMode
import com.example.liber.ui.reader.AnnotationRequest
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.readium.r2.shared.ExperimentalReadiumApi
import org.readium.r2.shared.publication.Publication

@OptIn(ExperimentalReadiumApi::class)
class HomeViewModel(application: Application) : AndroidViewModel(application) {

    private val bookRepository = BookRepository(AppDatabase.getDatabase(application).bookDao())
    private val scanSourceRepository =
        ScanSourceRepository(AppDatabase.getDatabase(application).scanSourceDao())
    private val bookImporter = BookImporter(application)

    private val sevenDaysMs = 7L * 24 * 60 * 60 * 1000

    // ── State flows ──────────────────────────────────────────────────────────

    val books: StateFlow<List<Book>> = bookRepository.getAllBooks()
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

    val scanSources: StateFlow<List<ScanSourceEntity>> = scanSourceRepository.getAllSources()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // ── Library UI preferences (persist across tab navigation) ───────────────

    private val _libraryViewMode = MutableStateFlow(LibraryViewMode.GRID)
    val libraryViewMode: StateFlow<LibraryViewMode> = _libraryViewMode

    private val _librarySortOption = MutableStateFlow(LibrarySortOption.RECENT)
    val librarySortOption: StateFlow<LibrarySortOption> = _librarySortOption

    fun setLibraryViewMode(mode: LibraryViewMode) {
        _libraryViewMode.value = mode
    }

    fun setLibrarySortOption(option: LibrarySortOption) {
        _librarySortOption.value = option
    }

    // Posted by MainActivity when the user taps Highlight / Add Note in the text-selection menu.
    private val _pendingAnnotationRequest = MutableStateFlow<AnnotationRequest?>(null)
    val pendingAnnotationRequest: StateFlow<AnnotationRequest?> = _pendingAnnotationRequest

    fun requestAnnotation(request: AnnotationRequest) {
        _pendingAnnotationRequest.value = request
    }

    fun clearPendingAnnotation() {
        _pendingAnnotationRequest.value = null
    }

    // ── Actions ──────────────────────────────────────────────────────────────

    /** Opens a book in the reader and records it as last opened. Returns null on failure. */
    suspend fun openBook(book: Book): Publication? = withContext(Dispatchers.IO) {
        bookRepository.updateLastOpenedAt(book.id, System.currentTimeMillis())
        try {
            getApplication<Application>().contentResolver.takePersistableUriPermission(
                book.fileUri,
                android.content.Intent.FLAG_GRANT_READ_URI_PERMISSION
            )
        } catch (_: Exception) { /* permission already held or not applicable */ }
        bookImporter.openPublication(book.fileUri)
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

    fun renameBook(bookId: String, newTitle: String) {
        viewModelScope.launch(Dispatchers.IO) {
            bookRepository.renameBook(bookId, newTitle)
        }
    }

    fun deleteBook(bookId: String) {
        viewModelScope.launch(Dispatchers.IO) {
            bookRepository.deleteBook(bookId)
        }
    }

    fun getAnnotationsForBook(bookId: String): Flow<List<AnnotationEntity>> =
        bookRepository.getAnnotationsForBook(bookId)

    fun saveAnnotation(annotation: AnnotationEntity) {
        viewModelScope.launch(Dispatchers.IO) { bookRepository.insertAnnotation(annotation) }
    }

    fun deleteAnnotation(annotationId: Long) {
        viewModelScope.launch(Dispatchers.IO) { bookRepository.deleteAnnotation(annotationId) }
    }

    fun getBookmarksForBook(bookId: String): Flow<List<BookmarkEntity>> =
        bookRepository.getBookmarksForBook(bookId)

    fun saveBookmark(bookmark: BookmarkEntity) {
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
                    tryTakePersistablePermission(uri)
                    val file = DocumentFile.fromSingleUri(getApplication(), uri) ?: return@forEach
                    val book = bookImporter.parseBook(file) ?: return@forEach
                    val isDuplicate = book.contentId
                        ?.let { bookRepository.getBookByContentId(it) } != null
                            || bookRepository.getBookByFileUri(uri.toString()) != null
                    if (!isDuplicate) {
                        bookRepository.insertBook(book.toEntity())
                    }
                }
            }
            _isLoading.value = false
        }
    }

    fun addScanSource(treeUri: Uri, folderName: String) {
        viewModelScope.launch(Dispatchers.IO) {
            scanSourceRepository.upsert(
                ScanSourceEntity(
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
