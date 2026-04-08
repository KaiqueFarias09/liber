package com.example.liber.data

import android.app.Application
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Typeface
import android.graphics.pdf.PdfRenderer
import android.net.Uri
import android.text.Layout
import android.text.StaticLayout
import android.text.TextPaint
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
        if (file.isDirectory) return parseAudiobookFolder(file)

        val extension = file.name?.substringAfterLast('.', "").orEmpty().lowercase()
        return when {
            extension == "pdf" -> parsePdf(file)
            isAudioFile(extension) -> parseSingleAudioFile(file)
            else -> parseEpub(file)
        }
    }

    private fun isAudioFile(extension: String): Boolean {
        return extension in setOf("mp3", "m4a", "m4b", "aac", "wav")
    }

    private suspend fun parseAudiobookFolder(dir: DocumentFile): Book {
        val title = dir.name ?: "Unknown Audiobook"
        val author = "Audiobook"
        val coverUri = saveBitmapToCache(generateFallbackCover(title, author), dir.name ?: "audio")

        return Book(
            id = UUID.randomUUID().toString(),
            title = title,
            author = author,
            coverUri = coverUri,
            fileUri = dir.uri,
            contentId = dir.uri.toString(), // URI acts as content ID for folders
            mediaType = "audio/mpeg", // Or readium audiobook manifest type?
        )
    }

    private suspend fun parseSingleAudioFile(file: DocumentFile): Book {
        val title = file.name?.substringBeforeLast('.') ?: "Unknown Audio"
        val author = "Audiobook"
        val coverUri = saveBitmapToCache(generateFallbackCover(title, author), file.name ?: "audio")
        val contentId = computeFileHash(file.uri) ?: file.uri.toString()

        return Book(
            id = UUID.randomUUID().toString(),
            title = title,
            author = author,
            coverUri = coverUri,
            fileUri = file.uri,
            contentId = contentId,
            mediaType = "audio/mpeg",
        )
    }

    private suspend fun parsePdf(file: DocumentFile): Book {
        val title = file.name?.substringBeforeLast('.') ?: "Unknown PDF"
        val author = "PDF Document"
        val coverUri = extractPdfCover(file.uri, file.name ?: "pdf") ?: run {
            saveBitmapToCache(generateFallbackCover(title, author), file.name ?: "pdf")
        }
        val contentId = computeFileHash(file.uri)
        return Book(
            id = UUID.randomUUID().toString(),
            title = title,
            author = author,
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
        val title = publication.metadata.title ?: "Unknown Title"
        val author = publication.metadata.authors.firstOrNull()?.name
        val coverUri = extractCover(publication, file.name ?: "cover", title, author)
        Book(
            id = UUID.randomUUID().toString(),
            title = title,
            author = author,
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

    private suspend fun extractCover(
        publication: Publication,
        fileName: String,
        title: String,
        author: String?
    ): Uri? = withContext(Dispatchers.IO) {
        val bitmap = publication.cover() ?: generateFallbackCover(title, author)
        saveBitmapToCache(bitmap, fileName)
    }

    private fun generateFallbackCover(title: String, author: String?): Bitmap {
        val width = 600
        val height = 900
        val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)

        // Background color based on title hash
        val colors = listOf(
            0xFF5C6BC0.toInt(), // Indigo 400
            0xFF26A69A.toInt(), // Teal 400
            0xFF7E57C2.toInt(), // Deep Purple 400
            0xFF42A5F5.toInt(), // Blue 400
            0xFF9CCC65.toInt(), // Light Green 400
            0xFF8D6E63.toInt(), // Brown 400
            0xFF78909C.toInt(), // Blue Grey 400
            0xFFEC407A.toInt(), // Pink 400
            0xFFFF7043.toInt(), // Deep Orange 400
            0xFF26C6DA.toInt()  // Cyan 400
        )
        val bgColor = colors[Math.abs(title.hashCode()) % colors.size]
        canvas.drawColor(bgColor)

        val textPaint = TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.WHITE
        }

        val padding = 60
        val maxWidth = width - (padding * 2)

        // Draw Title
        textPaint.textSize = 64f
        textPaint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)

        val titleLayout = StaticLayout.Builder.obtain(title, 0, title.length, textPaint, maxWidth)
            .setAlignment(Layout.Alignment.ALIGN_CENTER)
            .build()

        // Draw Author
        val authorLayout = author?.let {
            textPaint.textSize = 40f
            textPaint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
            StaticLayout.Builder.obtain(it, 0, it.length, textPaint, maxWidth)
                .setAlignment(Layout.Alignment.ALIGN_CENTER)
                .build()
        }

        val contentHeight =
            titleLayout.height + (if (authorLayout != null) authorLayout.height + 40 else 0)
        val startY = (height - contentHeight) / 2f

        canvas.save()
        canvas.translate(padding.toFloat(), startY)
        titleLayout.draw(canvas)
        canvas.restore()

        if (authorLayout != null) {
            canvas.save()
            canvas.translate(padding.toFloat(), startY + titleLayout.height + 40)
            authorLayout.draw(canvas)
            canvas.restore()
        }

        return bitmap
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
