package com.example.liber.feature.dictionary

import java.util.Locale

internal val languageMap = mapOf(
    "afr" to "Afrikaans", "deu" to "German", "eng" to "English",
    "ara" to "Arabic", "bre" to "Breton", "fra" to "French",
    "cat" to "Catalan", "fin" to "Finnish"
)

internal fun getLanguageName(tag: String): String {
    return languageMap[tag] ?: try {
        Locale.forLanguageTag(tag).getDisplayLanguage(Locale.ENGLISH)
            .replaceFirstChar { if (it.isLowerCase()) it.titlecase(Locale.ROOT) else it.toString() }
            .ifEmpty { tag.uppercase(Locale.ROOT) }
    } catch (e: Exception) {
        tag.uppercase(Locale.ROOT)
    }
}

internal fun formatBytes(bytes: Long?): String {
    if (bytes == null || bytes <= 0) return "0 B"
    val units = arrayOf("B", "KB", "MB", "GB", "TB")
    val digitGroups = (Math.log10(bytes.toDouble()) / Math.log10(1024.0)).toInt()
    return String.format(
        "%.1f %s",
        bytes / Math.pow(1024.0, digitGroups.toDouble()),
        units[digitGroups]
    )
}
