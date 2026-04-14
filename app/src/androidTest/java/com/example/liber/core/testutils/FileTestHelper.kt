package com.example.liber.core.testutils

import android.os.Environment
import androidx.test.platform.app.InstrumentationRegistry
import java.io.File
import java.io.FileOutputStream

object FileTestHelper {
    private val targetDir =
        Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)

    fun copyAssetsToEmulatorStorage() {
        val context = InstrumentationRegistry.getInstrumentation().context
        val assets = context.assets
        val filesToCopy = listOf("valid_book.epub", "invalid_file.pdf")

        filesToCopy.forEach { fileName ->
            assets.open(fileName).use { input ->
                val outFile = File(targetDir, fileName)
                FileOutputStream(outFile).use { output ->
                    input.copyTo(output)
                }
            }
        }

        val folderName = "test_audiobook"
        val folderDir = File(targetDir, folderName)
        if (!folderDir.exists()) folderDir.mkdirs()

        assets.list(folderName)?.forEach { fileName ->
            assets.open("$folderName/$fileName").use { input ->
                val outFile = File(folderDir, fileName)
                FileOutputStream(outFile).use { output ->
                    input.copyTo(output)
                }
            }
        }
    }

    fun cleanUpEmulatorStorage() {
        listOf("valid_book.epub", "invalid_file.pdf", "test_audiobook").forEach { name ->
            val file = File(targetDir, name)
            if (file.isDirectory) {
                file.deleteRecursively()
            } else {
                file.delete()
            }
        }
    }
}
