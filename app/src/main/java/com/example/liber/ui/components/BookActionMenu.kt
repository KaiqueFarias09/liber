package com.example.liber.ui.components

import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import com.adamglin.PhosphorIcons
import com.adamglin.phosphoricons.Fill
import com.adamglin.phosphoricons.Regular
import com.adamglin.phosphoricons.regular.BookOpen
import com.adamglin.phosphoricons.regular.CheckCircle
import com.adamglin.phosphoricons.regular.ListPlus
import com.adamglin.phosphoricons.regular.PencilSimple
import com.adamglin.phosphoricons.regular.PlusCircle
import com.adamglin.phosphoricons.regular.ShareNetwork
import com.adamglin.phosphoricons.regular.Trash
import com.example.liber.data.Book
import com.adamglin.phosphoricons.fill.Bookmark as BookmarkFill

@Composable
fun BookActionMenu(
    expanded: Boolean,
    book: Book,
    onDismiss: () -> Unit,
    onShare: () -> Unit,
    onToggleWantToRead: () -> Unit,
    onToggleFinished: () -> Unit,
    onRename: () -> Unit,
    onDelete: () -> Unit,
    deleteLabel: String = "Remove…",
    showAddToCollection: Boolean = false,
    onAddToCollection: (() -> Unit)? = null,
) {
    DropdownMenu(
        expanded = expanded,
        onDismissRequest = onDismiss,
    ) {
        DropdownMenuItem(
            text = { Text("Share") },
            leadingIcon = { Icon(PhosphorIcons.Regular.ShareNetwork, null) },
            onClick = { onShare(); onDismiss() },
        )
        DropdownMenuItem(
            text = { Text(if (book.wantToRead) "Remove from Want to Read" else "Add to Want to Read") },
            leadingIcon = {
                Icon(
                    if (book.wantToRead) PhosphorIcons.Fill.BookmarkFill
                    else PhosphorIcons.Regular.PlusCircle,
                    null,
                )
            },
            onClick = { onToggleWantToRead(); onDismiss() },
        )
        if (showAddToCollection && onAddToCollection != null) {
            DropdownMenuItem(
                text = { Text("Add to Collection") },
                leadingIcon = { Icon(PhosphorIcons.Regular.ListPlus, null) },
                onClick = { onAddToCollection(); onDismiss() },
            )
        }
        DropdownMenuItem(
            text = { Text(if (book.readingProgress == 100) "Mark as still reading" else "Mark as Finished") },
            leadingIcon = {
                Icon(
                    if (book.readingProgress == 100) PhosphorIcons.Regular.BookOpen
                    else PhosphorIcons.Regular.CheckCircle,
                    null,
                )
            },
            onClick = { onToggleFinished(); onDismiss() },
        )
        DropdownMenuItem(
            text = { Text("Rename…") },
            leadingIcon = { Icon(PhosphorIcons.Regular.PencilSimple, null) },
            onClick = { onRename(); onDismiss() },
        )
        DropdownMenuItem(
            text = { Text(deleteLabel, color = MaterialTheme.colorScheme.error) },
            leadingIcon = {
                Icon(
                    PhosphorIcons.Regular.Trash,
                    null,
                    tint = MaterialTheme.colorScheme.error
                )
            },
            onClick = { onDelete(); onDismiss() },
        )
    }
}
