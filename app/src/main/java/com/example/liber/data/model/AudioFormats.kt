package com.example.liber.data.model

object AudioFormats {
    val supportedExtensions = setOf(
        "mp3",
        "m4a",
        "m4b",
        "aac",
        "wav",
        "flac",
        "ogg",
        "opus",
        "amr",
        "awb",
        "3gp",
        "mka"
    )

    fun isSupported(extension: String?): Boolean {
        return extension?.lowercase() in supportedExtensions
    }

    fun isSupportedFile(fileName: String?): Boolean {
        val ext = fileName?.substringAfterLast('.', "")?.lowercase() ?: return false
        return isSupported(ext)
    }
}
