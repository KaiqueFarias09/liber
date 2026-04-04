package com.example.liber.ui.home

import android.app.Application
import android.graphics.Bitmap
import android.net.Uri
import androidx.documentfile.provider.DocumentFile
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.liber.data.AppDatabase
import com.example.liber.data.Book
import com.example.liber.data.BookEntity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
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
import java.util.*

@OptIn(ExperimentalReadiumApi::class)
class HomeViewModel(application: Application) : AndroidViewModel(application) {

    private val database = AppDatabase.getDatabase(application)
    private val bookDao = database.bookDao()

    val books: StateFlow<List<Book>> = bookDao.getAllBooks()
        .map { entities ->
            entities.map { entity ->
                Book(
                    id = entity.id,
                    title = entity.title,
                    author = entity.author,
                    coverUri = entity.coverPath?.let { Uri.fromFile(File(it)) },
                    fileUri = Uri.parse(entity.fileUri)
                )
            }
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading

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

    fun loadBooksFromUris(uris: List<Uri>) {
        viewModelScope.launch {
            _isLoading.value = true
            
            withContext(Dispatchers.IO) {
                uris.forEach { uri ->
                    try {
                        getApplication<Application>().contentResolver.takePersistableUriPermission(
                            uri,
                            android.content.Intent.FLAG_GRANT_READ_URI_PERMISSION
                        )
                    } catch (e: Exception) {
                        e.printStackTrace()
                    }

                    val file = DocumentFile.fromSingleUri(getApplication(), uri)
                    if (file != null) {
                        val book = parseBook(file)
                        if (book != null) {
                            bookDao.insertBook(
                                BookEntity(
                                    id = book.id,
                                    title = book.title,
                                    author = book.author,
                                    coverPath = book.coverUri?.path,
                                    fileUri = book.fileUri.toString()
                                )
                            )
                        }
                    }
                }
            }
            
            _isLoading.value = false
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

    private suspend fun extractCover(publication: Publication, fileName: String): Uri? {
        return withContext(Dispatchers.IO) {
            val bitmap = publication.cover() ?: return@withContext null
            
            val coverFile = File(getApplication<Application>().cacheDir, "cover_${fileName}_${System.currentTimeMillis()}.png")
            try {
                FileOutputStream(coverFile).use { out ->
                    bitmap.compress(Bitmap.CompressFormat.PNG, 100, out)
                }
                Uri.fromFile(coverFile)
            } catch (e: Exception) {
                e.printStackTrace()
                null
            }
        }
    }

    private fun copyToTempFile(uri: Uri): File {
        val context = getApplication<Application>()
        val inputStream = context.contentResolver.openInputStream(uri)
        val tempFile = File(context.cacheDir, "temp_${System.currentTimeMillis()}.epub")
        FileOutputStream(tempFile).use { outputStream ->
            inputStream?.copyTo(outputStream)
            inputStream?.close()
        }
        return tempFile
    }
}
