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

    fun getMimeType(fileName: String?): String {
        val ext = fileName?.substringAfterLast('.', "")?.lowercase() ?: ""
        return when (ext) {
            "flac" -> "audio/flac"
            "m4a", "m4b" -> "audio/mp4"
            "wav" -> "audio/wav"
            "ogg", "opus" -> "audio/ogg"
            "aac" -> "audio/aac"
            "amr" -> "audio/amr"
            "awb" -> "audio/amr-wb"
            "3gp" -> "audio/3gpp"
            "mka" -> "audio/x-matroska"
            else -> "audio/mpeg"
        }
    }
}
