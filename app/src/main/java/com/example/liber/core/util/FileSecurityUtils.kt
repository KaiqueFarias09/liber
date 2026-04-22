package com.example.liber.core.util

import android.content.Context
import android.net.Uri
import androidx.documentfile.provider.DocumentFile
import com.example.liber.core.logging.AppLogger
import java.io.File
import java.io.FileOutputStream

object FileSecurityUtils {
    private const val MAX_FILE_SIZE_BYTES = 500 * 1024 * 1024L

    fun copyToTempFileSafe(
        context: Context,
        uri: Uri,
        prefix: String = "temp_",
        appLogger: AppLogger? = null,
    ): File? {
        val documentFile = DocumentFile.fromSingleUri(context, uri)
        val rawName = documentFile?.name ?: "file"
        val extension = rawName.substringAfterLast('.', "epub")
        val safeName = rawName.substringBeforeLast('.').sanitizeForFileName()

        val tempFile = File(
            context.cacheDir,
            "${prefix}${safeName}_${System.currentTimeMillis()}.$extension"
        )

        var totalBytes = 0L

        return try {
            context.contentResolver.openInputStream(uri)?.use { input ->
                FileOutputStream(tempFile).use { output ->
                    val buffer = ByteArray(8192)
                    var bytesRead: Int
                    while (input.read(buffer).also { bytesRead = it } != -1) {
                        totalBytes += bytesRead

                        if (totalBytes > MAX_FILE_SIZE_BYTES) {
                            appLogger?.warn(
                                "Rejected temporary file copy because it exceeded $MAX_FILE_SIZE_BYTES bytes",
                                tag = "FileSecurityUtils",
                            )
                            tempFile.delete()
                            return null
                        }

                        output.write(buffer, 0, bytesRead)
                    }
                }
            }
            tempFile
        } catch (e: Exception) {
            e.rethrowIfCancellation()
            appLogger?.error("Failed to copy content URI to temp file", tag = "FileSecurityUtils", throwable = e)
            tempFile.delete()
            null
        }
    }
}
