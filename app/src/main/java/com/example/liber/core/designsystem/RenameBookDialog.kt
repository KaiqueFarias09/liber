package com.example.liber.core.designsystem

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier

@Composable
fun RenameBookDialog(
    currentTitle: String,
    onConfirm: (String) -> Unit,
    onDismiss: () -> Unit,
) {
    var title by remember { mutableStateOf(currentTitle) }
    LiberDialog(
        onDismissRequest = onDismiss,
        title = "Rename book",
        confirmLabel = "Save",
        onConfirm = { onConfirm(title) },
        confirmEnabled = title.isNotBlank(),
        dismissLabel = "Cancel",
        onDismiss = onDismiss,
    ) {
        LiberTextField(
            value = title,
            onValueChange = { title = it },
            label = "Title",
            singleLine = true,
            modifier = Modifier.fillMaxWidth(),
        )
    }
}
