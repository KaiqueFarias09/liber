package com.example.liber.ui.home

import android.app.Application
import android.graphics.Bitmap
import android.graphics.pdf.PdfRenderer
import android.net.Uri
import androidx.documentfile.provider.DocumentFile
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.liber.data.AnnotationEntity
import com.example.liber.data.AppDatabase
import com.example.liber.data.Book
import com.example.liber.data.BookEntity
import com.example.liber.data.BookRepository
import com.example.liber.data.BookmarkEntity
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
import org.readium.r2.shared.publication.services.cover
import org.readium.r2.shared.util.asset.AssetRetriever
import org.readium.r2.shared.util.http.DefaultHttpClient
import org.readium.r2.shared.util.toUrl
import org.readium.r2.streamer.PublicationOpener
import org.readium.r2.streamer.parser.DefaultPublicationParser
import java.io.File
import java.io.FileOutputStream
import java.util.UUID

@OptIn(ExperimentalReadiumApi::class)
class HomeViewModel(application: Application) : AndroidViewModel(application) {

    private val bookRepository = BookRepository(AppDatabase.getDatabase(application).bookDao())

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
        val file = copyToTempFile(book.fileUri)
        val asset = assetRetriever.retrieve(file.toUrl()).getOrNull() ?: return@withContext null
        publicationOpener.open(asset, allowUserInteraction = false).getOrNull()
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
                    if (bookRepository.getBookByFileUri(uri.toString()) == null) {
                        val file = DocumentFile.fromSingleUri(getApplication(), uri) ?: return@forEach
                        val book = parseBook(file) ?: return@forEach
                        bookRepository.insertBook(book.toEntity())
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
        val extension = file.name?.substringAfterLast('.', "").orEmpty().lowercase()
        if (extension == "pdf") {
            val coverUri = extractPdfCover(file.uri, file.name ?: "pdf")
            return Book(
                id = UUID.randomUUID().toString(),
                title = file.name?.substringBeforeLast('.') ?: "Unknown PDF",
                author = "PDF Document",
                coverUri = coverUri,
                fileUri = file.uri
            )
        }

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
            saveBitmapToCache(bitmap, fileName)
        }

    private suspend fun extractPdfCover(uri: Uri, fileName: String): Uri? =
        withContext(Dispatchers.IO) {
            try {
                val context = getApplication<Application>()
                context.contentResolver.openFileDescriptor(uri, "r")?.use { pfd ->
                    val renderer = PdfRenderer(pfd)
                    if (renderer.pageCount > 0) {
                        renderer.openPage(0).use { page ->
                            val bitmap = Bitmap.createBitmap(page.width, page.height, Bitmap.Config.ARGB_8888)
                            page.render(bitmap, null, null, PdfRenderer.Page.RENDER_MODE_FOR_DISPLAY)
                            return@withContext saveBitmapToCache(bitmap, fileName)
                        }
                    }
                    renderer.close()
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
            null
        }

    private fun saveBitmapToCache(bitmap: Bitmap, fileName: String): Uri? {
        val coverFile = File(
            getApplication<Application>().cacheDir,
            "cover_${fileName}_${System.currentTimeMillis()}.png"
        )
        return try {
            FileOutputStream(coverFile).use { it.compress(bitmap) }
            Uri.fromFile(coverFile)
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    private fun copyToTempFile(uri: Uri): File {
        val context = getApplication<Application>()
        val documentFile = DocumentFile.fromSingleUri(context, uri)
        val name = documentFile?.name ?: "file"
        val extension = name.substringAfterLast('.', "epub")
        val tempFile = File(context.cacheDir, "temp_${System.currentTimeMillis()}.$extension")
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

private fun FileOutputStream.compress(bitmap: Bitmap) {
    bitmap.compress(Bitmap.CompressFormat.PNG, 100, this)
}
