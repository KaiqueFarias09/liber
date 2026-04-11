package com.example.liber.core.designsystem

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import com.example.liber.R
import com.example.liber.core.util.UiText

@Composable
fun DeleteBookConfirmationDialog(
    bookTitle: String,
    action: UiText,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit,
) {
    val cleanAction = action.asString().removeSuffix("…")
    val isRemove = cleanAction.contains("Remove", ignoreCase = true)

    val title = if (isRemove) {
        UiText.StringResource(R.string.dialog_title_remove_book)
    } else {
        UiText.StringResource(R.string.dialog_title_delete_book)
    }

    LiberDialog(
        onDismissRequest = onDismiss,
        title = title,
        confirmLabel = UiText.DynamicString(cleanAction),
        onConfirm = onConfirm,
        confirmLabelColor = MaterialTheme.colorScheme.error,
        dismissLabel = UiText.StringResource(R.string.action_cancel),
    ) {
        Text(
            text = stringResource(
                R.string.dialog_message_delete_book,
                cleanAction.lowercase(),
                bookTitle
            ),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}
