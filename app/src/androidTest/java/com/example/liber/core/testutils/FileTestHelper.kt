package com.example.liber.core.testutils

import android.os.Environment
import androidx.test.platform.app.InstrumentationRegistry
import java.io.File
import java.io.FileOutputStream

object FileTestHelper {
    private val targetDir =
        Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)

    fun copyAssetsToEmulatorStorage() {
        val targetContext = InstrumentationRegistry.getInstrumentation().targetContext
        val testContext = InstrumentationRegistry.getInstrumentation().context
        val assets = testContext.assets
        val filesToCopy = mapOf(
            "pg43-images-3.epub" to "Jekyll.epub",
            "pg2554-images-3.epub" to "Crime.epub",
            "pg2701-images-3.epub" to "MobyDick.epub"
        )

        val scannedPaths = mutableListOf<String>()

        filesToCopy.forEach { (assetName, targetName) ->
            try {
                assets.open(assetName).use { input ->
                    if (!targetDir.exists()) targetDir.mkdirs()
                    val outFile = File(targetDir, targetName)
                    FileOutputStream(outFile).use { output ->
                        input.copyTo(output)
                    }
                    scannedPaths.add(outFile.absolutePath)
                    println("Successfully copied $assetName to ${outFile.absolutePath}")
                }
            } catch (e: Exception) {
                println("Failed to copy $assetName: ${e.message}")
            }
        }

        val folderName = "test_audiobook"
        val folderDir = File(targetDir, folderName)
        if (!folderDir.exists()) folderDir.mkdirs()

        assets.list(folderName)?.forEach { fileName ->
            try {
                assets.open("$folderName/$fileName").use { input ->
                    val outFile = File(folderDir, fileName)
                    FileOutputStream(outFile).use { output ->
                        input.copyTo(output)
                    }
                    scannedPaths.add(outFile.absolutePath)
                }
            } catch (e: Exception) {
                println("Failed to copy $fileName: ${e.message}")
            }
        }

        // Trigger media scan to make files visible to system picker
        if (scannedPaths.isNotEmpty()) {
            android.media.MediaScannerConnection.scanFile(
                targetContext,
                scannedPaths.toTypedArray(),
                null,
                null
            )
        }
    }

    fun cleanUpEmulatorStorage() {
        listOf(
            "pg43-images-3.epub",
            "pg2554-images-3.epub",
            "pg2701-images-3.epub",
            "Jekyll.epub",
            "Crime.epub",
            "MobyDick.epub",
            "test_audiobook"
        ).forEach { name ->
            val file = File(targetDir, name)
            if (file.exists()) {
                if (file.isDirectory) {
                    file.deleteRecursively()
                } else {
                    file.delete()
                }
            }
        }
    }
}
