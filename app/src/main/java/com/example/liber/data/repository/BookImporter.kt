package com.example.liber.data.repository

import android.app.Application
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.media.MediaMetadataRetriever
import android.net.Uri
import androidx.documentfile.provider.DocumentFile
import com.example.liber.core.logging.AppLogger
import com.example.liber.core.util.FileSecurityUtils
import com.example.liber.core.util.rethrowIfCancellation
import com.example.liber.core.util.sanitizeForFileName
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
class BookImporter(
    private val application: Application,
    private val appLogger: AppLogger,
) {

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

            val epubFile =
                FileSecurityUtils.copyToTempFileSafe(
                    context = application,
                    uri = uri,
                    appLogger = appLogger,
                ) ?: return@withContext null
            val asset =
                assetRetriever.retrieve(epubFile.toUrl()).getOrNull() ?: return@withContext null
            publicationOpener.open(asset, allowUserInteraction = false).getOrNull()
        } catch (e: Exception) {
            e.rethrowIfCancellation()
            appLogger.error("Failed to open publication", tag = "BookImporter", throwable = e)
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
            coverUri = null
        }

        book.copy(
            title = title,
            author = author,
            coverUri = coverUri
        )
    }

    /** Extracts ALBUM and ARTIST text tags from an audio file. */
    private suspend fun extractTextMetadata(file: DocumentFile): Pair<String?, String?> =
        withContext(Dispatchers.IO) {
            val retriever = MediaMetadataRetriever()
            try {
                application.contentResolver.openFileDescriptor(file.uri, "r")?.use { pfd ->
                    retriever.setDataSource(pfd.fileDescriptor)
                }
                val album = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_ALBUM)
                val artist = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_ARTIST)
                Pair(album, artist)
            } catch (e: Exception) {
                e.rethrowIfCancellation()
                appLogger.warn("Failed to extract audio metadata", tag = "BookImporter", throwable = e)
                Pair(null, null)
            } finally {
                retriever.release()
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

    suspend fun extractEmbeddedCover(file: DocumentFile, cacheName: String): Uri? =
        withContext(Dispatchers.IO) {
            val retriever = MediaMetadataRetriever()
            try {
                application.contentResolver.openFileDescriptor(file.uri, "r")?.use { pfd ->
                    retriever.setDataSource(pfd.fileDescriptor)
                }

                val art = retriever.embeddedPicture ?: return@withContext null
                val bitmap =
                    BitmapFactory.decodeByteArray(art, 0, art.size) ?: return@withContext null
                saveBitmapToCache(bitmap, cacheName)
            } catch (e: Exception) {
                e.rethrowIfCancellation()
                appLogger.warn("Failed to extract embedded cover", tag = "BookImporter", throwable = e)
                null
            } finally {
                retriever.release()
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
        val epubFile = FileSecurityUtils.copyToTempFileSafe(
            context = application,
            uri = file.uri,
            appLogger = appLogger,
        ) ?: return null
        val asset = assetRetriever.retrieve(epubFile.toUrl()).getOrNull() ?: return null
        val publication = publicationOpener.open(asset, allowUserInteraction = false).getOrNull()
            ?: return null
        val title = publication.metadata.title ?: "Unknown Title"
        val author = publication.metadata.authors.firstOrNull()?.name
        val language = publication.metadata.languages.firstOrNull()
        val coverUri = extractCover(publication, file.name ?: "cover")
        Book(
            id = UUID.randomUUID().toString(),
            title = title,
            author = author,
            coverUri = coverUri,
            fileUri = file.uri,
            contentId = publication.metadata.identifier ?: computeFileHash(file.uri),
            language = language,
        )
    } catch (e: Exception) {
        e.rethrowIfCancellation()
        appLogger.error("Failed to parse EPUB", tag = "BookImporter", throwable = e)
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
        e.rethrowIfCancellation()
        appLogger.warn("Failed to compute file hash", tag = "BookImporter", throwable = e)
        null
    }

    private suspend fun extractCover(
        publication: Publication,
        fileName: String
    ): Uri? = withContext(Dispatchers.IO) {
        val bitmap = publication.cover() ?: return@withContext null
        saveBitmapToCache(bitmap, fileName)
    }

    private fun saveBitmapToCache(bitmap: Bitmap, fileName: String): Uri? {
        val safeFileName = fileName.sanitizeForFileName()
        val coverFile = File(
            application.cacheDir,
            "cover_${safeFileName}_${System.currentTimeMillis()}.png"
        )
        return try {
            FileOutputStream(coverFile).use { bitmap.compress(Bitmap.CompressFormat.PNG, 100, it) }
            Uri.fromFile(coverFile)
        } catch (e: Exception) {
            e.rethrowIfCancellation()
            appLogger.warn("Failed to save cover image to cache", tag = "BookImporter", throwable = e)
            null
        }
    }

}
