package com.example.liber.core.designsystem

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
import com.example.liber.core.util.UiText
import com.example.liber.data.model.Book
import com.adamglin.phosphoricons.fill.Bookmark as BookmarkFill

@Composable
fun BookActionMenu(
    expanded: Boolean,
    book: Book,
    onDismiss: () -> Unit,
    onShare: () -> Unit,
    onToggleWantToRead: () -> Unit,
    onToggleFinished: () -> Unit,
    onShowDetails: () -> Unit,
    onDelete: () -> Unit,
    deleteLabel: UiText = UiText.DynamicString("Remove…"),
    showAddToCollection: Boolean = false,
    onAddToCollection: (() -> Unit)? = null,
) {
    LiberDropdownMenu(
        expanded = expanded,
        onDismissRequest = onDismiss,
    ) {
        LiberContextMenuItem(
            label = UiText.DynamicString("Share"),
            icon = PhosphorIcons.Regular.ShareNetwork,
            onClick = { onShare(); onDismiss() },
        )
        LiberContextMenuItem(
            label = if (book.wantToRead) UiText.DynamicString("Remove from Want to Read") else UiText.DynamicString(
                "Add to Want to Read"
            ),
            icon = if (book.wantToRead) PhosphorIcons.Fill.BookmarkFill else PhosphorIcons.Regular.PlusCircle,
            onClick = { onToggleWantToRead(); onDismiss() },
        )
        if (showAddToCollection && onAddToCollection != null) {
            LiberContextMenuItem(
                label = UiText.DynamicString("Add to Collection"),
                icon = PhosphorIcons.Regular.ListPlus,
                onClick = { onAddToCollection(); onDismiss() },
            )
        }
        LiberContextMenuItem(
            label = if (book.readingProgress == 100) UiText.DynamicString("Mark as still reading") else UiText.DynamicString(
                "Mark as Finished"
            ),
            icon = if (book.readingProgress == 100) PhosphorIcons.Regular.BookOpen else PhosphorIcons.Regular.CheckCircle,
            onClick = { onToggleFinished(); onDismiss() },
        )
        LiberContextMenuItem(
            label = UiText.DynamicString("Edit / Details…"),
            icon = PhosphorIcons.Regular.PencilSimple,
            onClick = { onShowDetails(); onDismiss() },
        )
        LiberContextMenuDivider()
        LiberContextMenuItem(
            label = deleteLabel,
            icon = PhosphorIcons.Regular.Trash,
            destructive = true,
            onClick = { onDelete(); onDismiss() },
        )
    }
}
