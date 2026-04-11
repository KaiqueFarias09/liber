package com.example.liber.core.intent

import android.content.Context
import android.content.Intent
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Centralizes all Intent creation and dispatching, keeping UI code clean.
 * Inject this wherever you need to fire share or open intents.
 */
@Singleton
class AppIntentHandler @Inject constructor(
    @ApplicationContext private val context: Context,
) {

    fun shareText(text: String, author: String? = null, title: String? = null) {
        val shareBody = buildString {
            append(text)
            if (!author.isNullOrBlank() || !title.isNullOrBlank()) {
                append("\n\n—")
                if (!title.isNullOrBlank()) append(" $title")
                if (!author.isNullOrBlank()) append(", $author")
            }
        }
        val intent = Intent(Intent.ACTION_SEND).apply {
            type = "text/plain"
            putExtra(Intent.EXTRA_TEXT, shareBody)
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        context.startActivity(
            Intent.createChooser(intent, "Compartilhar via").apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
        )
    }

    fun shareBookFile(fileUri: android.net.Uri) {
        val intent = Intent(Intent.ACTION_SEND).apply {
            type = "application/epub+zip"
            putExtra(Intent.EXTRA_STREAM, fileUri)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        context.startActivity(
            Intent.createChooser(intent, "Share Book").apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
        )
    }
}
