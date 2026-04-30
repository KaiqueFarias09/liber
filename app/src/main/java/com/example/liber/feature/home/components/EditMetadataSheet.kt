package com.example.liber.feature.home.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import com.example.liber.R
import com.example.liber.core.designsystem.LiberButton
import com.example.liber.core.designsystem.LiberTextField
import com.example.liber.core.util.UiText
import com.example.liber.data.model.Book

@Composable
fun EditMetadataSheet(
    book: Book,
    onSave: (String, String?, String?) -> Unit
) {
    var title by remember { mutableStateOf(book.title) }
    var author by remember { mutableStateOf(book.author ?: "") }
    var narrator by remember { mutableStateOf(book.narrator ?: "") }

    Column(
        modifier = Modifier
            .padding(horizontal = 20.dp)
            .padding(bottom = 40.dp)
            .padding(top = 8.dp),
        verticalArrangement = Arrangement.spacedBy(20.dp)
    ) {
        MetadataInputField(
            label = UiText.StringResource(R.string.field_label_title),
            value = title,
            onValueChange = { title = it },
            placeholder = UiText.StringResource(R.string.placeholder_book_title)
        )

        MetadataInputField(
            label = UiText.StringResource(R.string.field_label_author),
            value = author,
            onValueChange = { author = it },
            placeholder = UiText.StringResource(R.string.placeholder_author_name)
        )

        if (book.isAudiobook) {
            MetadataInputField(
                label = UiText.StringResource(R.string.field_label_narrator),
                value = narrator,
                onValueChange = { narrator = it },
                placeholder = UiText.StringResource(R.string.placeholder_narrated_by)
            )
        }

        Spacer(modifier = Modifier.height(8.dp))

        LiberButton(
            text = UiText.StringResource(R.string.action_save_changes),
            onClick = { onSave(title, author.ifBlank { null }, narrator.ifBlank { null }) },
            modifier = Modifier.fillMaxWidth()
        )
    }
}

@Composable
fun MetadataInputField(
    label: UiText,
    value: String,
    onValueChange: (String) -> Unit,
    placeholder: UiText
) {
    val focusManager = LocalFocusManager.current

    LiberTextField(
        value = value,
        onValueChange = onValueChange,
        label = label,
        placeholder = placeholder,
        singleLine = true,
        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
        keyboardActions = KeyboardActions(onDone = { focusManager.clearFocus() })
    )
}
