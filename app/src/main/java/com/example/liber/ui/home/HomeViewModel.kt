package com.example.liber.ui.home

import android.app.Application
import android.graphics.Bitmap
import android.net.Uri
import androidx.documentfile.provider.DocumentFile
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.liber.data.AnnotationEntity
import com.example.liber.data.BookmarkEntity
import com.example.liber.data.AppDatabase
import com.example.liber.data.Book
import com.example.liber.data.BookEntity
import com.example.liber.ui.reader.AnnotationRequest
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import androidx.core.net.toUri
import org.readium.r2.shared.ExperimentalReadiumApi
import org.readium.r2.shared.publication.Publication
import org.readium.r2.shared.publication.services.cover
import org.readium.r2.shared.util.asset.AssetRetriever
import org.readium.r2.shared.util.http.DefaultHttpClient
import org.readium.r2.shared.util.toUrl
import org.readium.r2.streamer.PublicationOpener
import org.readium.r2.streamer.parser.DefaultPublicationParser
import java.io.File
import java.io.FileOutputStream
import java.util.*

@OptIn(ExperimentalReadiumApi::class)
class HomeViewModel(application: Application) : AndroidViewModel(application) {

    private val database = AppDatabase.getDatabase(application)
    private val bookDao = database.bookDao()

    private val sevenDaysMs = 7L * 24 * 60 * 60 * 1000

    private val httpClient = DefaultHttpClient()
    private val assetRetriever = AssetRetriever(getApplication<Application>().contentResolver, httpClient)
    private val publicationOpener = PublicationOpener(
        publicationParser = DefaultPublicationParser(
            context = getApplication(),
            httpClient = httpClient,
            assetRetriever = assetRetriever,
            pdfFactory = null
        )
    )

    // ── State flows ──────────────────────────────────────────────────────────

    val books: StateFlow<List<Book>> = bookDao.getAllBooks()
        .map { it.map(BookEntity::toBook) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val continueReadingBooks: StateFlow<List<Book>> = bookDao
        .getContinueReadingBooks(System.currentTimeMillis() - sevenDaysMs)
        .map { it.map(BookEntity::toBook) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val wantToReadBooks: StateFlow<List<Book>> = bookDao.getWantToReadBooks()
        .map { it.map(BookEntity::toBook) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val previousBooks: StateFlow<List<Book>> = bookDao
        .getPreviousBooks(System.currentTimeMillis() - sevenDaysMs)
        .map { it.map(BookEntity::toBook) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading

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
        bookDao.updateLastOpenedAt(book.id, System.currentTimeMillis())
        try {
            getApplication<Application>().contentResolver.takePersistableUriPermission(
                book.fileUri,
                android.content.Intent.FLAG_GRANT_READ_URI_PERMISSION
            )
        } catch (_: Exception) { /* permission already held or not applicable */ }
        val file = copyToTempFile(book.fileUri)
        val asset = assetRetriever.retrieve(file.toUrl()).getOrNull() ?: return@withContext null
        publicationOpener.open(asset, allowUserInteraction = false).getOrNull()
    }

    fun saveLocator(bookId: String, locatorJson: String, progress: Int) {
        viewModelScope.launch(Dispatchers.IO) {
            bookDao.updateLastLocator(bookId, locatorJson, progress)
        }
    }

    fun toggleWantToRead(bookId: String, currentValue: Boolean) {
        viewModelScope.launch(Dispatchers.IO) {
            bookDao.updateWantToRead(bookId, !currentValue)
        }
    }

    fun toggleFinished(bookId: String, isFinished: Boolean) {
        viewModelScope.launch(Dispatchers.IO) {
            val progress = if (isFinished) 0 else 100
            bookDao.updateLastLocator(bookId, null, progress)
        }
    }

    fun markAsFinished(bookId: String) {
        viewModelScope.launch(Dispatchers.IO) {
            bookDao.updateLastLocator(bookId, null, 100)
        }
    }

    fun renameBook(bookId: String, newTitle: String) {
        viewModelScope.launch(Dispatchers.IO) {
            bookDao.renameBook(bookId, newTitle)
        }
    }

    fun deleteBook(bookId: String) {
        viewModelScope.launch(Dispatchers.IO) {
            bookDao.deleteBook(bookId)
        }
    }

    fun getAnnotationsForBook(bookId: String): Flow<List<AnnotationEntity>> =
        bookDao.getAnnotationsForBook(bookId)

    fun saveAnnotation(annotation: AnnotationEntity) {
        viewModelScope.launch(Dispatchers.IO) { bookDao.insertAnnotation(annotation) }
    }

    fun deleteAnnotation(annotationId: Long) {
        viewModelScope.launch(Dispatchers.IO) { bookDao.deleteAnnotation(annotationId) }
    }

    fun getBookmarksForBook(bookId: String): Flow<List<BookmarkEntity>> =
        bookDao.getBookmarksForBook(bookId)

    fun saveBookmark(bookmark: BookmarkEntity) {
        viewModelScope.launch(Dispatchers.IO) { bookDao.insertBookmark(bookmark) }
    }

    fun deleteBookmark(bookmarkId: Long) {
        viewModelScope.launch(Dispatchers.IO) { bookDao.deleteBookmark(bookmarkId) }
    }

    fun loadBooksFromUris(uris: List<Uri>) {
        viewModelScope.launch {
            _isLoading.value = true
            withContext(Dispatchers.IO) {
                uris.forEach { uri ->
                    tryTakePersistablePermission(uri)
                    if (bookDao.getBookByFileUri(uri.toString()) == null) {
                        val file = DocumentFile.fromSingleUri(getApplication(), uri) ?: return@forEach
                        val book = parseBook(file) ?: return@forEach
                        bookDao.insertBook(book.toEntity())
                    }
                }
            }
            _isLoading.value = false
        }
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

    private suspend fun parseBook(file: DocumentFile): Book? {
        return try {
            val tempFile = copyToTempFile(file.uri)
            val asset = assetRetriever.retrieve(tempFile.toUrl()).getOrNull() ?: return null
            val publication = publicationOpener.open(asset, allowUserInteraction = false).getOrNull() ?: return null
            val coverUri = extractCover(publication, file.name ?: "cover")
            Book(
                id = UUID.randomUUID().toString(),
                title = publication.metadata.title ?: "Unknown Title",
                author = publication.metadata.authors.firstOrNull()?.name,
                coverUri = coverUri,
                fileUri = file.uri
            )
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    private suspend fun extractCover(publication: Publication, fileName: String): Uri? =
        withContext(Dispatchers.IO) {
            val bitmap = publication.cover() ?: return@withContext null
            val coverFile = File(
                getApplication<Application>().cacheDir,
                "cover_${fileName}_${System.currentTimeMillis()}.png"
            )
            try {
                FileOutputStream(coverFile).use { it.compress(bitmap) }
                Uri.fromFile(coverFile)
            } catch (e: Exception) {
                e.printStackTrace()
                null
            }
        }

    private fun copyToTempFile(uri: Uri): File {
        val context = getApplication<Application>()
        val tempFile = File(context.cacheDir, "temp_${System.currentTimeMillis()}.epub")
        context.contentResolver.openInputStream(uri)?.use { input ->
            FileOutputStream(tempFile).use { input.copyTo(it) }
        }
        return tempFile
    }

    // ── Mapping extensions ───────────────────────────────────────────────────

    private fun Book.toEntity() = BookEntity(
        id = id,
        title = title,
        author = author,
        coverPath = coverUri?.path,
        fileUri = fileUri.toString()
    )
}

private fun BookEntity.toBook() = Book(
    id = id,
    title = title,
    author = author,
    coverUri = coverPath?.let { Uri.fromFile(File(it)) },
    fileUri = fileUri.toUri(),
    lastOpenedAt = lastOpenedAt,
    wantToRead = wantToRead,
    readingProgress = readingProgress,
    lastLocator = lastLocator
)

private fun FileOutputStream.compress(bitmap: Bitmap) {
    bitmap.compress(Bitmap.CompressFormat.PNG, 100, this)
}
