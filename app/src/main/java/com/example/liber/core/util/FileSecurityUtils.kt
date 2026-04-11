package com.example.liber.core.util

import android.content.Context
import android.net.Uri
import androidx.documentfile.provider.DocumentFile
import java.io.File
import java.io.FileOutputStream

object FileSecurityUtils {
    private const val MAX_FILE_SIZE_BYTES = 500 * 1024 * 1024L

    fun copyToTempFileSafe(context: Context, uri: Uri, prefix: String = "temp_"): File? {
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
                            tempFile.delete()
                            return null
                        }

                        output.write(buffer, 0, bytesRead)
                    }
                }
            }
            tempFile
        } catch (e: Exception) {
            e.printStackTrace()
            tempFile.delete()
            null
        }
    }
}
