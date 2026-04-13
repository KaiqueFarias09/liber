package com.example.liber.core.intent

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import com.example.liber.data.model.Book
import com.example.liber.data.repository.BookRepository
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

sealed class IncomingIntentAction {
    data class OpenAudiobook(val book: Book) : IncomingIntentAction()
    data class ImportAndOpen(val uri: Uri) : IncomingIntentAction()
    object Unhandled : IncomingIntentAction()
}

/**
 * Centralizes all Intent creation and dispatching, keeping UI code clean.
 * Inject this wherever you need to fire share, open, or handle incoming intents.
 */
@Singleton
class AppIntentHandler @Inject constructor(
    @ApplicationContext private val context: Context,
    private val bookRepository: BookRepository,
) {

    suspend fun resolveIncomingIntent(intent: Intent): IncomingIntentAction {
        val action = intent.action
        val type = intent.type

        return when {
            "com.example.liber.OPEN_BOOK" == action -> {
                val bookId = intent.getStringExtra("bookId")
                    ?: return IncomingIntentAction.Unhandled
                val book = bookRepository.getBookByContentId(bookId)
                    ?: bookRepository.getAllBooksList().find { it.id == bookId }
                    ?: return IncomingIntentAction.Unhandled
                IncomingIntentAction.OpenAudiobook(book)
            }

            Intent.ACTION_VIEW == action -> {
                val uri = intent.data ?: return IncomingIntentAction.Unhandled
                IncomingIntentAction.ImportAndOpen(uri)
            }

            Intent.ACTION_SEND == action && type != null -> {
                val uri: Uri? = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                    intent.getParcelableExtra(Intent.EXTRA_STREAM, Uri::class.java)
                } else {
                    @Suppress("DEPRECATION")
                    intent.getParcelableExtra(Intent.EXTRA_STREAM)
                }
                uri?.let { IncomingIntentAction.ImportAndOpen(it) }
                    ?: IncomingIntentAction.Unhandled
            }

            else -> IncomingIntentAction.Unhandled
        }
    }

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

    fun shareBookFile(fileUri: Uri) {
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
