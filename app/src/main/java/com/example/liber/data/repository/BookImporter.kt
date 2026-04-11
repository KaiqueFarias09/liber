package com.example.liber.data.repository

import android.app.Application
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Typeface
import android.media.MediaMetadataRetriever
import android.net.Uri
import android.text.Layout
import android.text.StaticLayout
import android.text.TextPaint
import androidx.core.graphics.createBitmap
import androidx.core.graphics.withTranslation
import androidx.documentfile.provider.DocumentFile
import com.example.liber.data.model.AudioFormats
import com.example.liber.data.model.Book
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.readium.r2.shared.ExperimentalReadiumApi
import org.readium.r2.shared.InternalReadiumApi
import org.readium.r2.shared.publication.Href
import org.readium.r2.shared.publication.Link
import org.readium.r2.shared.publication.LocalizedString
import org.readium.r2.shared.publication.Manifest
import org.readium.r2.shared.publication.Metadata
import org.readium.r2.shared.publication.Publication
import org.readium.r2.shared.publication.services.cover
import org.readium.r2.shared.util.AbsoluteUrl
import org.readium.r2.shared.util.Try
import org.readium.r2.shared.util.Url
import org.readium.r2.shared.util.asset.AssetRetriever
import org.readium.r2.shared.util.content.ContentResolverError
import org.readium.r2.shared.util.data.Container
import org.readium.r2.shared.util.data.ReadError
import org.readium.r2.shared.util.http.DefaultHttpClient
import org.readium.r2.shared.util.mediatype.MediaType
import org.readium.r2.shared.util.resource.FailureResource
import org.readium.r2.shared.util.resource.Resource
import org.readium.r2.shared.util.toUrl
import org.readium.r2.streamer.PublicationOpener
import org.readium.r2.streamer.parser.DefaultPublicationParser
import java.io.File
import java.io.FileOutputStream
import java.security.MessageDigest
import java.util.UUID

@OptIn(ExperimentalReadiumApi::class, InternalReadiumApi::class)
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

    suspend fun openPublication(uri: Uri): Publication? = withContext(Dispatchers.IO) {
        try {
            val file = if (uri.toString().contains("tree")) {
                DocumentFile.fromTreeUri(application, uri)
            } else {
                DocumentFile.fromSingleUri(application, uri)
            } ?: return@withContext null

            if (file.isDirectory) {
                return@withContext createAudiobookPublication(file)
            }

            val extension = file.name?.substringAfterLast('.', "").orEmpty().lowercase()
            if (AudioFormats.isSupported(extension)) {
                return@withContext createAudiobookPublication(file)
            }

            val tempFile = copyToTempFile(uri)
            val asset =
                assetRetriever.retrieve(tempFile.toUrl()).getOrNull() ?: return@withContext null
            publicationOpener.open(asset, allowUserInteraction = false).getOrNull()
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    suspend fun parseBook(file: DocumentFile): Book? = withContext(Dispatchers.IO) {
        if (file.isDirectory) return@withContext parseAudiobookFolder(file)

        val extension = file.name?.substringAfterLast('.', "").orEmpty().lowercase()
        when {
            AudioFormats.isSupported(extension) -> parseSingleAudioFile(file)
            else -> parseEpub(file)
        }
    }

    private suspend fun parseAudiobookFolder(dir: DocumentFile): Book {
        val title = dir.name ?: "Unknown Audiobook"

        val initialBook = Book(
            id = UUID.randomUUID().toString(),
            title = title,
            author = null,
            coverUri = null,
            fileUri = dir.uri,
            contentId = dir.uri.toString(),
            mediaType = "audio/mpeg",
        )
        // Fill metadata (including cover) immediately during import
        return fillAudiobookMetadata(initialBook)
    }

    suspend fun fillAudiobookMetadata(book: Book): Book = withContext(Dispatchers.IO) {
        val dir = DocumentFile.fromTreeUri(application, book.fileUri) ?: return@withContext book
        val files = dir.listFiles()
        val firstAudio = files
            .filter {
                it.isFile && AudioFormats.isSupported(
                    it.name?.substringAfterLast('.', "").orEmpty().lowercase()
                )
            }
            .minByOrNull { it.name ?: "" }

        val (albumTitle, albumArtist) = if (firstAudio != null) extractTextMetadata(firstAudio)
        else Pair(null, null)

        val title = albumTitle?.takeIf { it.isNotBlank() } ?: book.title
        val author = albumArtist?.takeIf { it.isNotBlank() }

        // Find cover
        val imageExtensions = setOf("jpg", "jpeg", "png", "webp")
        val preferredNames = setOf("cover", "folder", "front", "album", "artwork", "art")

        // Preferred: canonical cover name
        var coverUri = files.find { file ->
            val name = file.name?.substringBeforeLast('.')?.lowercase() ?: ""
            val ext = file.name?.substringAfterLast('.', "")?.lowercase() ?: ""
            file.isFile && name in preferredNames && ext in imageExtensions
        }?.uri

        // Fallback 1: any image file in the folder
        if (coverUri == null) {
            coverUri = files.find { file ->
                val ext = file.name?.substringAfterLast('.', "")?.lowercase() ?: ""
                file.isFile && ext in imageExtensions
            }?.uri
        }

        // Fallback 2: embedded cover
        if (coverUri == null && firstAudio != null) {
            coverUri = extractEmbeddedCover(firstAudio, title)
        }

        // Fallback 3: generated cover
        if (coverUri == null) {
            coverUri = saveBitmapToCache(generateFallbackCover(title, author), dir.name ?: "audio")
        }

        book.copy(
            title = title,
            author = author,
            coverUri = coverUri
        )
    }

    /** Extracts ALBUM and ARTIST text tags from an audio file via a small temp-file copy. */
    private suspend fun extractTextMetadata(file: DocumentFile): Pair<String?, String?> =
        withContext(Dispatchers.IO) {
            val tempFile = File(application.cacheDir, "meta_txt_${System.currentTimeMillis()}")
            val retriever = MediaMetadataRetriever()
            try {
                application.contentResolver.openInputStream(file.uri)?.use { input ->
                    FileOutputStream(tempFile).use { out ->
                        val buf = ByteArray(65_536)
                        var remaining = 64 * 1024 // 64 KB is enough for all text tags
                        while (remaining > 0) {
                            val n = input.read(buf, 0, minOf(buf.size, remaining))
                            if (n < 0) break
                            out.write(buf, 0, n)
                            remaining -= n
                        }
                    }
                }
                retriever.setDataSource(tempFile.absolutePath)
                val album = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_ALBUM)
                val artist = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_ARTIST)
                Pair(album, artist)
            } catch (e: Exception) {
                e.printStackTrace()
                Pair(null, null)
            } finally {
                retriever.release()
                tempFile.delete()
            }
        }

    private fun createAudiobookPublication(file: DocumentFile): Publication? {
        val audioFiles = if (file.isDirectory) {
            file.listFiles()
                .filter {
                    it.isFile && AudioFormats.isSupported(
                        it.name?.substringAfterLast('.', "").orEmpty().lowercase()
                    )
                }
                .sortedBy { it.name }
        } else {
            listOf(file)
        }

        if (audioFiles.isEmpty()) return null

        val links = audioFiles.mapNotNull { f ->
            f.name?.let { name ->
                Url.fromDecodedPath("/$name")?.let { url ->
                    Link(
                        href = Href(url),
                        mediaType = MediaType(AudioFormats.getMimeType(name))
                    )
                }
            }
        }

        val manifest = Manifest(
            metadata = Metadata(
                localizedTitle = LocalizedString(
                    file.name?.substringBeforeLast('.') ?: "Unknown Audiobook"
                )
            ),
            readingOrder = links
        )

        val container = object : Container<Resource> {
            override val sourceUrl: AbsoluteUrl? = null

            override val entries: Set<Url> = audioFiles.mapNotNull { f ->
                f.name?.let { Url.fromDecodedPath("/$it") }
            }.toSet()

            override fun get(url: Url): Resource? {
                val fileName = url.path?.removePrefix("/")
                val targetFile = audioFiles.find { it.name == fileName }
                return if (targetFile != null) {
                    ContentResolverResource(application.contentResolver, targetFile.uri)
                } else {
                    FailureResource(ReadError.Access(ContentResolverError.FileNotFound()))
                }
            }

            override fun close() {}
        }

        return Publication(manifest = manifest, container = container)
    }

    private suspend fun getBestAudiobookCover(
        dir: DocumentFile,
        firstAudio: DocumentFile?,
        title: String,
        author: String?
    ): Uri? {
        return findCoverImageInFolder(dir)
            ?: firstAudio?.let { extractEmbeddedCover(it, title) }
            ?: saveBitmapToCache(generateFallbackCover(title, author), dir.name ?: "audio")
    }

    private fun findCoverImageInFolder(dir: DocumentFile): Uri? {
        val imageExtensions = setOf("jpg", "jpeg", "png", "webp")
        val preferredNames = setOf("cover", "folder", "front", "album", "artwork", "art")
        val files = dir.listFiles()

        // Preferred: canonical cover name
        files.find { file ->
            val name = file.name?.substringBeforeLast('.')?.lowercase() ?: ""
            val ext = file.name?.substringAfterLast('.', "")?.lowercase() ?: ""
            file.isFile && name in preferredNames && ext in imageExtensions
        }?.uri?.let { return it }

        // Fallback: any image file in the folder
        return files.find { file ->
            val ext = file.name?.substringAfterLast('.', "")?.lowercase() ?: ""
            file.isFile && ext in imageExtensions
        }?.uri
    }

    suspend fun extractEmbeddedCover(file: DocumentFile, cacheName: String): Uri? =
        withContext(Dispatchers.IO) {
            // Copy the first 5 MB of the audio file to a temp path so MediaMetadataRetriever
            // can use setDataSource(absolutePath) — the most reliable variant across Android versions.
            // 5 MB is more than enough to contain any ID3v2 tag with album art.
            val tempFile = File(application.cacheDir, "meta_tmp_${System.currentTimeMillis()}")
            val retriever = MediaMetadataRetriever()
            try {
                val written = application.contentResolver.openInputStream(file.uri)?.use { input ->
                    FileOutputStream(tempFile).use { out ->
                        val buf = ByteArray(65_536)
                        var remaining = 5 * 1024 * 1024
                        while (remaining > 0) {
                            val n = input.read(buf, 0, minOf(buf.size, remaining))
                            if (n < 0) break
                            out.write(buf, 0, n)
                            remaining -= n
                        }
                    }
                    true
                } ?: false
                if (!written) return@withContext null

                retriever.setDataSource(tempFile.absolutePath)
                val art = retriever.embeddedPicture ?: return@withContext null
                val bitmap =
                    BitmapFactory.decodeByteArray(art, 0, art.size) ?: return@withContext null
                saveBitmapToCache(bitmap, cacheName)
            } catch (e: Exception) {
                e.printStackTrace()
                null
            } finally {
                retriever.release()
                tempFile.delete()
            }
        }

    /**
     * Minimal Resource implementation for ContentResolver URIs.
     * In a production Readium app, you'd use their built-in ContentResolverFetcher / Resource if available.
     */
    private class ContentResolverResource(
        private val contentResolver: android.content.ContentResolver,
        private val uri: Uri,
    ) : Resource {
        override val sourceUrl: AbsoluteUrl? = null

        override suspend fun properties(): Try<Resource.Properties, ReadError> =
            Try.success(Resource.Properties())

        override suspend fun length(): Try<Long, ReadError> = try {
            contentResolver.openFileDescriptor(uri, "r")?.use {
                Try.success(it.statSize)
            } ?: Try.failure(ReadError.Access(ContentResolverError.FileNotFound()))
        } catch (e: Exception) {
            Try.failure(ReadError.Access(ContentResolverError.IO(e)))
        }

        override suspend fun read(range: LongRange?): Try<ByteArray, ReadError> = try {
            contentResolver.openInputStream(uri)?.use { input ->
                if (range != null) {
                    input.skip(range.first)
                    val length = (range.last - range.first + 1).toInt()
                    val bytes = ByteArray(length)
                    val bytesRead = input.read(bytes)
                    val finalBytes =
                        if (bytesRead < length) bytes.copyOf(maxOf(0, bytesRead)) else bytes
                    Try.success(finalBytes)
                } else {
                    Try.success(input.readBytes())
                }
            } ?: Try.failure(ReadError.Access(ContentResolverError.FileNotFound()))
        } catch (e: Exception) {
            Try.failure(ReadError.Access(ContentResolverError.IO(e)))
        }

        override fun close() {}
    }

    private suspend fun parseSingleAudioFile(file: DocumentFile): Book {
        val title = file.name?.substringBeforeLast('.') ?: "Unknown Audio"
        val author = "Audiobook"
        val coverUri = extractEmbeddedCover(file, title)
            ?: saveBitmapToCache(generateFallbackCover(title, author), file.name ?: "audio")
        val contentId = computeFileHash(file.uri) ?: file.uri.toString()

        return Book(
            id = UUID.randomUUID().toString(),
            title = title,
            author = author,
            coverUri = coverUri,
            fileUri = file.uri,
            contentId = contentId,
            mediaType = AudioFormats.getMimeType(file.name),
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
        val bitmap = createBitmap(width, height)
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

        canvas.withTranslation(padding.toFloat(), startY) {
            titleLayout.draw(this)
        }

        if (authorLayout != null) {
            canvas.withTranslation(padding.toFloat(), startY + titleLayout.height + 40) {
                authorLayout.draw(this)
            }
        }

        return bitmap
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
