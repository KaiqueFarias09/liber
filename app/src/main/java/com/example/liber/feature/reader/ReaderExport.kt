package com.example.liber.feature.reader

import android.content.Context
import android.content.Intent
import android.widget.Toast
import androidx.core.content.FileProvider
import com.example.liber.data.model.Annotation
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File

fun exportAnnotations(
    context: Context,
    scope: CoroutineScope,
    bookTitle: String,
    annotations: List<Annotation>,
    format: String = "markdown"
) {
    if (annotations.isEmpty()) return

    scope.launch(Dispatchers.IO) {
        val content = when (format) {
            "markdown" -> buildString {
                append("# $bookTitle\n\n")
                append("## Annotations\n\n")
                annotations.sortedBy { it.createdAt }.forEach { annotation ->
                    if (!annotation.text.isNullOrBlank()) {
                        append("> ${annotation.text}\n\n")
                    }
                    if (!annotation.note.isNullOrBlank()) {
                        append("${annotation.note}\n\n")
                    }
                    append("---\n\n")
                }
            }

            "json" -> {
                val items = annotations.map { annotation ->
                    val text = annotation.text?.replace("\"", "\\\"")?.replace("\n", "\\n") ?: ""
                    val note = annotation.note?.replace("\"", "\\\"")?.replace("\n", "\\n") ?: ""
                    "{\"text\":\"$text\",\"note\":\"$note\",\"type\":\"${annotation.type}\"}"
                }
                "[\n  ${items.joinToString(",\n  ")}\n]"
            }

            "html" -> buildString {
                append("<!DOCTYPE html><html><head><meta charset=\"UTF-8\"><title>$bookTitle - Annotations</title>")
                append("<style>body{font-family:sans-serif;max-width:800px;margin:40px auto;padding:0 20px;line-height:1.6;}")
                append("blockquote{border-left:4px solid #ccc;padding-left:16px;margin:20px 0;font-style:italic;}")
                append("hr{border:0;border-top:1px solid #eee;margin:40px 0;}</style></head><body>")
                append("<h1>$bookTitle</h1>")
                annotations.sortedBy { it.createdAt }.forEach { annotation ->
                    if (!annotation.text.isNullOrBlank()) {
                        append("<blockquote>${annotation.text}</blockquote>")
                    }
                    if (!annotation.note.isNullOrBlank()) {
                        append("<p>${annotation.note}</p>")
                    }
                    append("<hr>")
                }
                append("</body></html>")
            }

            else -> buildString { // txt
                append("$bookTitle - Annotations\n\n")
                annotations.sortedBy { it.createdAt }.forEach { annotation ->
                    if (!annotation.text.isNullOrBlank()) {
                        append("\"${annotation.text}\"\n")
                    }
                    if (!annotation.note.isNullOrBlank()) {
                        append("Note: ${annotation.note}\n")
                    }
                    append("\n-------------------\n\n")
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

        val fileName = "${bookTitle.replace(Regex("[^a-zA-Z0-9.-]"), "_")}_annotations.$extension"
        val file = File(context.cacheDir, fileName)
        try {
            file.writeText(content)
            val uri = FileProvider.getUriForFile(
                context, "${context.packageName}.fileprovider", file
            )

            val intent = Intent(Intent.ACTION_SEND).apply {
                type = mimeType
                putExtra(Intent.EXTRA_SUBJECT, "$bookTitle - Annotations")
                putExtra(Intent.EXTRA_STREAM, uri)
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }
            context.startActivity(Intent.createChooser(intent, "Export Annotations"))
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
