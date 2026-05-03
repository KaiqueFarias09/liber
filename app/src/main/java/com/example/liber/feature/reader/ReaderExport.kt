package com.example.liber.feature.reader

import android.content.Context
import android.content.Intent
import android.widget.Toast
import androidx.core.content.FileProvider
import com.example.liber.data.model.Annotation
import com.example.liber.data.model.Bookmark
import com.example.liber.feature.notebook.BookNotebookData
import com.example.liber.feature.notebook.NotebookNotebookItem
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

fun exportReaderData(
    context: Context,
    scope: CoroutineScope,
    bookTitle: String,
    annotations: List<Annotation>,
    bookmarks: List<Bookmark>,
    format: String = "markdown"
) {
    if (annotations.isEmpty() && bookmarks.isEmpty()) return

    scope.launch(Dispatchers.IO) {
        val dateFormat = SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault())

        val content = when (format) {
            "markdown" -> buildString {
                append("# $bookTitle\n\n")

                if (annotations.isNotEmpty()) {
                    append("## Annotations\n\n")
                    annotations.sortedBy { it.createdAt }.forEach { annotation ->
                        if (!annotation.text.isNullOrBlank()) {
                            append("> ${annotation.text}\n\n")
                        }
                        if (!annotation.note.isNullOrBlank()) {
                            append("${annotation.note}\n\n")
                        }
                        append("*Added on ${dateFormat.format(Date(annotation.createdAt))}*\n\n")
                        append("---\n\n")
                    }
                }

                if (bookmarks.isNotEmpty()) {
                    append("## Bookmarks\n\n")
                    bookmarks.sortedBy { it.createdAt }.forEach { bookmark ->
                        append("### ${bookmark.chapter ?: "Unknown Chapter"}\n")
                        append("*Bookmarked on ${dateFormat.format(Date(bookmark.createdAt))}*\n\n")
                        append("---\n\n")
                    }
                }
            }

            "json" -> {
                val annotationItems = annotations.map { annotation ->
                    val text = annotation.text?.replace("\"", "\\\"")?.replace("\n", "\\n") ?: ""
                    val note = annotation.note?.replace("\"", "\\\"")?.replace("\n", "\\n") ?: ""
                    "{\"type\":\"annotation\",\"text\":\"$text\",\"note\":\"$note\",\"annotationType\":\"${annotation.type}\",\"createdAt\":${annotation.createdAt},\"chapter\":\"${annotation.chapter ?: ""}\"}"
                }
                val bookmarkItems = bookmarks.map { bookmark ->
                    "{\"type\":\"bookmark\",\"chapter\":\"${bookmark.chapter ?: ""}\",\"createdAt\":${bookmark.createdAt}}"
                }
                val allItems = annotationItems + bookmarkItems
                "[\n  ${allItems.joinToString(",\n  ")}\n]"
            }

            "html" -> buildString {
                append("<!DOCTYPE html><html><head><meta charset=\"UTF-8\"><title>$bookTitle - Export</title>")
                append("<style>body{font-family:sans-serif;max-width:800px;margin:40px auto;padding:0 20px;line-height:1.6;}")
                append("blockquote{border-left:4px solid #ccc;padding-left:16px;margin:20px 0;font-style:italic;}")
                append("hr{border:0;border-top:1px solid #eee;margin:40px 0;}")
                append(".date{font-size:0.8em;color:#666;}</style></head><body>")
                append("<h1>$bookTitle</h1>")

                if (annotations.isNotEmpty()) {
                    append("<h2>Annotations</h2>")
                    annotations.sortedBy { it.createdAt }.forEach { annotation ->
                        if (!annotation.text.isNullOrBlank()) {
                            append("<blockquote>${annotation.text}</blockquote>")
                        }
                        if (!annotation.note.isNullOrBlank()) {
                            append("<p>${annotation.note}</p>")
                        }
                        append("<p class=\"date\">Added on ${dateFormat.format(Date(annotation.createdAt))}</p>")
                        append("<hr>")
                    }
                }

                if (bookmarks.isNotEmpty()) {
                    append("<h2>Bookmarks</h2>")
                    bookmarks.sortedBy { it.createdAt }.forEach { bookmark ->
                        append("<h3>${bookmark.chapter ?: "Unknown Chapter"}</h3>")
                        append("<p class=\"date\">Bookmarked on ${dateFormat.format(Date(bookmark.createdAt))}</p>")
                        append("<hr>")
                    }
                }
                append("</body></html>")
            }

            else -> buildString { // txt
                append("$bookTitle - Export\n\n")

                if (annotations.isNotEmpty()) {
                    append("ANNOTATIONS\n\n")
                    annotations.sortedBy { it.createdAt }.forEach { annotation ->
                        if (!annotation.text.isNullOrBlank()) {
                            append("\"${annotation.text}\"\n")
                        }
                        if (!annotation.note.isNullOrBlank()) {
                            append("Note: ${annotation.note}\n")
                        }
                        append("Date: ${dateFormat.format(Date(annotation.createdAt))}\n")
                        append("\n-------------------\n\n")
                    }
                }

                if (bookmarks.isNotEmpty()) {
                    append("BOOKMARKS\n\n")
                    bookmarks.sortedBy { it.createdAt }.forEach { bookmark ->
                        append("Chapter: ${bookmark.chapter ?: "Unknown"}\n")
                        append("Date: ${dateFormat.format(Date(bookmark.createdAt))}\n")
                        append("\n-------------------\n\n")
                    }
                }
            }
        }

        val extension = when (format) {
            "markdown" -> "md"
            "json" -> "json"
            "html" -> "html"
            else -> "txt"
        }
        val mimeType = when (format) {
            "markdown" -> "text/markdown"
            "json" -> "application/json"
            "html" -> "text/html"
            else -> "text/plain"
        }

        val fileName = "${bookTitle.replace(Regex("[^a-zA-Z0-9.-]"), "_")}_export.$extension"
        val file = File(context.cacheDir, fileName)
        try {
            file.writeText(content)
            val uri = FileProvider.getUriForFile(
                context, "${context.packageName}.fileprovider", file
            )

            val intent = Intent(Intent.ACTION_SEND).apply {
                type = mimeType
                putExtra(Intent.EXTRA_SUBJECT, "$bookTitle - Export")
                putExtra(Intent.EXTRA_STREAM, uri)
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }
            context.startActivity(Intent.createChooser(intent, "Export Data"))
        } catch (e: Exception) {
            withContext(Dispatchers.Main) {
                Toast.makeText(
                    context,
                    "Failed to export: ${e.message}",
                    Toast.LENGTH_LONG
                ).show()
            }
        }
    }
}

fun exportNotebookData(
    context: Context,
    scope: CoroutineScope,
    data: List<BookNotebookData>,
    format: String = "markdown"
) {
    if (data.isEmpty()) return

    scope.launch(Dispatchers.IO) {
        val dateFormat = SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault())
        val exportTitle = if (data.size == 1) data[0].bookTitle else "Liber Notebook Export"

        val content = when (format) {
            "markdown" -> buildString {
                append("# $exportTitle\n\n")
                data.forEach { book ->
                    append("## ${book.bookTitle}\n")
                    if (!book.author.isNullOrBlank()) append("Author: ${book.author}\n")
                    append("\n")

                    book.items.forEach { item ->
                        when (item) {
                            is NotebookNotebookItem.Highlight -> {
                                if (!item.quote.isNullOrBlank()) {
                                    append("> ${item.quote}\n\n")
                                }
                                if (!item.note.isNullOrBlank()) {
                                    append("${item.note}\n\n")
                                }
                                append("*Added on ${dateFormat.format(Date(item.createdAt))} in ${item.chapter ?: "Unknown Chapter"}*\n\n")
                            }

                            is NotebookNotebookItem.BookmarkItem -> {
                                append("### Bookmark: ${item.chapter ?: "Unknown Chapter"}\n")
                                append("*Bookmarked on ${dateFormat.format(Date(item.createdAt))}*\n\n")
                            }
                        }
                        append("---\n\n")
                    }
                }
            }

            "json" -> {
                val jsonItems = data.map { book ->
                    val items = book.items.map { item ->
                        when (item) {
                            is NotebookNotebookItem.Highlight -> {
                                val quote =
                                    item.quote?.replace("\"", "\\\"")?.replace("\n", "\\n") ?: ""
                                val note =
                                    item.note?.replace("\"", "\\\"")?.replace("\n", "\\n") ?: ""
                                "{\"type\":\"highlight\",\"quote\":\"$quote\",\"note\":\"$note\",\"chapter\":\"${item.chapter ?: ""}\",\"createdAt\":${item.createdAt}}"
                            }

                            is NotebookNotebookItem.BookmarkItem -> {
                                "{\"type\":\"bookmark\",\"chapter\":\"${item.chapter ?: ""}\",\"createdAt\":${item.createdAt}}"
                            }
                        }
                    }
                    "{\"bookTitle\":\"${
                        book.bookTitle.replace(
                            "\"",
                            "\\\""
                        )
                    }\",\"author\":\"${book.author ?: ""}\",\"items\":[${items.joinToString(",")}]}"
                }
                "[\n  ${jsonItems.joinToString(",\n  ")}\n]"
            }

            else -> buildString { // txt fallback (simple text)
                append("$exportTitle\n\n")
                data.forEach { book ->
                    append("BOOK: ${book.bookTitle}\n")
                    if (!book.author.isNullOrBlank()) append("AUTHOR: ${book.author}\n")
                    append("-------------------\n\n")

                    book.items.forEach { item ->
                        when (item) {
                            is NotebookNotebookItem.Highlight -> {
                                if (!item.quote.isNullOrBlank()) append("\"${item.quote}\"\n")
                                if (!item.note.isNullOrBlank()) append("Note: ${item.note}\n")
                                append("Chapter: ${item.chapter ?: "Unknown"}\n")
                            }

                            is NotebookNotebookItem.BookmarkItem -> {
                                append("BOOKMARK: ${item.chapter ?: "Unknown"}\n")
                            }
                        }
                        append("Date: ${dateFormat.format(Date(item.createdAt))}\n\n")
                    }
                    append("===================\n\n")
                }
            }
        }

        val extension = if (format == "json") "json" else if (format == "markdown") "md" else "txt"
        val mimeType =
            if (format == "json") "application/json" else if (format == "markdown") "text/markdown" else "text/plain"

        val fileName = "${exportTitle.replace(Regex("[^a-zA-Z0-9.-]"), "_")}_export.$extension"
        val file = File(context.cacheDir, fileName)
        try {
            file.writeText(content)
            val uri = FileProvider.getUriForFile(
                context, "${context.packageName}.fileprovider", file
            )

            val intent = Intent(Intent.ACTION_SEND).apply {
                type = mimeType
                putExtra(Intent.EXTRA_SUBJECT, exportTitle)
                putExtra(Intent.EXTRA_STREAM, uri)
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }
            context.startActivity(Intent.createChooser(intent, "Export Notebook"))
        } catch (e: Exception) {
            withContext(Dispatchers.Main) {
                Toast.makeText(context, "Failed to export: ${e.message}", Toast.LENGTH_LONG).show()
            }
        }
    }
}
