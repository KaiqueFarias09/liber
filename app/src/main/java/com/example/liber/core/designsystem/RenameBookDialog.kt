package com.example.liber.core.designsystem

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import com.example.liber.R
import com.example.liber.core.util.InputValidator
import com.example.liber.core.util.UiText

@Composable
fun RenameBookDialog(
    currentTitle: String,
    onConfirm: (String) -> Unit,
    onDismiss: () -> Unit,
) {
    var title by remember { mutableStateOf(currentTitle) }
    LiberDialog(
        onDismissRequest = onDismiss,
        title = UiText.StringResource(R.string.dialog_title_rename_book),
        confirmLabel = UiText.StringResource(R.string.action_save),
        onConfirm = { onConfirm(title) },
        confirmEnabled = title.isNotBlank(),
        dismissLabel = UiText.StringResource(R.string.action_cancel),
    ) {
        LiberTextField(
            value = title,
            onValueChange = { title = InputValidator.validatedBookTitle(it) },
            label = UiText.StringResource(R.string.field_label_title),
            singleLine = true,
            modifier = Modifier.fillMaxWidth(),
        )
    }
}
