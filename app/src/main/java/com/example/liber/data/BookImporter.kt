package com.example.liber.data

import android.app.Application
import android.graphics.Bitmap
import android.graphics.pdf.PdfRenderer
import android.net.Uri
import androidx.documentfile.provider.DocumentFile
import kotlinx.coroutines.Dispatchers
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
import java.security.MessageDigest
import java.util.UUID

@OptIn(ExperimentalReadiumApi::class)
class BookImporter(private val application: Application) {

    private val httpClient = DefaultHttpClient()
    private val assetRetriever = AssetRetriever(application.contentResolver, httpClient)
    private val publicationOpener = PublicationOpener(
        publicationParser = DefaultPublicationParser(
            context = application,
            httpClient = httpClient,
            assetRetriever = assetRetriever,
            pdfFactory = null,
        )
    )

    suspend fun openPublication(uri: Uri): Publication? = try {
        val tempFile = copyToTempFile(uri)
        val asset = assetRetriever.retrieve(tempFile.toUrl()).getOrNull() ?: return null
        publicationOpener.open(asset, allowUserInteraction = false).getOrNull()
    } catch (e: Exception) {
        e.printStackTrace()
        null
    }

    suspend fun parseBook(file: DocumentFile): Book? {
        val extension = file.name?.substringAfterLast('.', "").orEmpty().lowercase()
        return if (extension == "pdf") parsePdf(file) else parseEpub(file)
    }

    private suspend fun parsePdf(file: DocumentFile): Book {
        val coverUri = extractPdfCover(file.uri, file.name ?: "pdf")
        val contentId = computeFileHash(file.uri)
        return Book(
            id = UUID.randomUUID().toString(),
            title = file.name?.substringBeforeLast('.') ?: "Unknown PDF",
            author = "PDF Document",
            coverUri = coverUri,
            fileUri = file.uri,
            contentId = contentId,
        )
    }

    private suspend fun parseEpub(file: DocumentFile): Book? = try {
        val tempFile = copyToTempFile(file.uri)
        val asset = assetRetriever.retrieve(tempFile.toUrl()).getOrNull() ?: return null
        val publication = publicationOpener.open(asset, allowUserInteraction = false).getOrNull()
            ?: return null
        val coverUri = extractCover(publication, file.name ?: "cover")
        Book(
            id = UUID.randomUUID().toString(),
            title = publication.metadata.title ?: "Unknown Title",
            author = publication.metadata.authors.firstOrNull()?.name,
            coverUri = coverUri,
            fileUri = file.uri,
            contentId = publication.metadata.identifier ?: computeFileHash(file.uri),
        )
    } catch (e: Exception) {
        e.printStackTrace()
        null
    }

    private fun computeFileHash(uri: Uri): String? = try {
        val digest = MessageDigest.getInstance("SHA-256")
        application.contentResolver.openInputStream(uri)?.use { input ->
            val buffer = ByteArray(65536) // first 64 KB
            val bytesRead = input.read(buffer)
            if (bytesRead > 0) digest.update(buffer, 0, bytesRead)
        }
        digest.digest().joinToString("") { "%02x".format(it) }
    } catch (e: Exception) {
        null
    }

    private suspend fun extractCover(publication: Publication, fileName: String): Uri? =
        withContext(Dispatchers.IO) {
            val bitmap = publication.cover() ?: return@withContext null
            saveBitmapToCache(bitmap, fileName)
        }

    private suspend fun extractPdfCover(uri: Uri, fileName: String): Uri? =
        withContext(Dispatchers.IO) {
            try {
                application.contentResolver.openFileDescriptor(uri, "r")?.use { pfd ->
                    val renderer = PdfRenderer(pfd)
                    if (renderer.pageCount > 0) {
                        renderer.openPage(0).use { page ->
                            val bitmap = Bitmap.createBitmap(
                                page.width, page.height, Bitmap.Config.ARGB_8888
                            )
                            page.render(
                                bitmap,
                                null,
                                null,
                                PdfRenderer.Page.RENDER_MODE_FOR_DISPLAY
                            )
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
            application.cacheDir,
            "cover_${fileName}_${System.currentTimeMillis()}.png"
        )
        return try {
            FileOutputStream(coverFile).use { bitmap.compress(Bitmap.CompressFormat.PNG, 100, it) }
            Uri.fromFile(coverFile)
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    private fun copyToTempFile(uri: Uri): File {
        val documentFile = DocumentFile.fromSingleUri(application, uri)
        val name = documentFile?.name ?: "file"
        val extension = name.substringAfterLast('.', "epub")
        val tempFile = File(application.cacheDir, "temp_${System.currentTimeMillis()}.$extension")
        application.contentResolver.openInputStream(uri)?.use { input ->
            FileOutputStream(tempFile).use { input.copyTo(it) }
        }
        return tempFile
    }
}
