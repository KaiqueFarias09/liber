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
import com.example.liber.R
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
    deleteLabel: UiText = UiText.StringResource(R.string.action_remove_ellipsis),
    showAddToCollection: Boolean = false,
    onAddToCollection: (() -> Unit)? = null,
) {
    LiberDropdownMenu(
        expanded = expanded,
        onDismissRequest = onDismiss,
    ) {
        LiberContextMenuItem(
            label = UiText.StringResource(R.string.action_share),
            icon = PhosphorIcons.Regular.ShareNetwork,
            onClick = { onShare(); onDismiss() },
        )
        LiberContextMenuItem(
            label = if (book.wantToRead) UiText.StringResource(R.string.action_remove_want_to_read)
            else UiText.StringResource(R.string.action_add_want_to_read),
            icon = if (book.wantToRead) PhosphorIcons.Fill.BookmarkFill else PhosphorIcons.Regular.PlusCircle,
            onClick = { onToggleWantToRead(); onDismiss() },
        )
        if (showAddToCollection && onAddToCollection != null) {
            LiberContextMenuItem(
                label = UiText.StringResource(R.string.action_add_to_collection),
                icon = PhosphorIcons.Regular.ListPlus,
                onClick = { onAddToCollection(); onDismiss() },
            )
        }
        LiberContextMenuItem(
            label = if (book.readingProgress == 100) UiText.StringResource(R.string.action_mark_still_reading)
            else UiText.StringResource(R.string.action_mark_finished),
            icon = if (book.readingProgress == 100) PhosphorIcons.Regular.BookOpen else PhosphorIcons.Regular.CheckCircle,
            onClick = { onToggleFinished(); onDismiss() },
        )
        LiberContextMenuItem(
            label = UiText.StringResource(R.string.action_edit_details),
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
