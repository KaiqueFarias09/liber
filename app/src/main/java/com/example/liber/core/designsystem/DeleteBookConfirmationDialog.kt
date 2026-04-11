package com.example.liber.core.designsystem

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable

@Composable
fun DeleteBookConfirmationDialog(
    bookTitle: String,
    actionLabel: String,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit,
) {
    val cleanAction = actionLabel.removeSuffix("…")
    val title = if (cleanAction.contains("Remove", ignoreCase = true)) {
        "Remove book?"
    } else {
        "Delete book?"
    }

    LiberDialog(
        onDismissRequest = onDismiss,
        title = title,
        confirmLabel = cleanAction,
        onConfirm = onConfirm,
        confirmLabelColor = MaterialTheme.colorScheme.error,
        dismissLabel = "Cancel",
        onDismiss = onDismiss,
    ) {
        Text(
            text = "Are you sure you want to ${cleanAction.lowercase()} \"$bookTitle\"?",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}
