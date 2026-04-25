package com.example.liber.feature.collections.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import com.example.liber.R
import com.example.liber.core.designsystem.BookCover
import com.example.liber.core.designsystem.CoverStyle
import com.example.liber.core.designsystem.LiberDialog
import com.example.liber.core.designsystem.LiberTextField
import com.example.liber.core.util.InputValidator
import com.example.liber.core.util.UiText
import com.example.liber.data.model.Book

@Composable
fun CollectionNameDialog(
    title: UiText,
    initialName: String,
    onConfirm: (String) -> Unit,
    onDismiss: () -> Unit,
) {
    var name by remember { mutableStateOf(initialName) }
    val focusRequester = remember { FocusRequester() }

    LiberDialog(
        onDismissRequest = onDismiss,
        title = title,
        confirmLabel = UiText.StringResource(R.string.action_save),
        onConfirm = { if (name.isNotBlank()) onConfirm(name) },
        confirmEnabled = name.isNotBlank(),
        dismissLabel = UiText.StringResource(R.string.action_cancel),
    ) {
        LiberTextField(
            value = name,
            onValueChange = { name = InputValidator.validatedCollectionName(it) },
            label = UiText.StringResource(R.string.field_label_name),
            singleLine = true,
            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
            keyboardActions = KeyboardActions(onDone = { if (name.isNotBlank()) onConfirm(name) }),
            modifier = Modifier
                .fillMaxWidth()
                .focusRequester(focusRequester)
                .onGloballyPositioned { focusRequester.requestFocus() },
        )
    }
}

@Composable
fun DeleteCollectionDialog(
    collectionName: String,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit,
) {
    LiberDialog(
        onDismissRequest = onDismiss,
        title = UiText.StringResource(R.string.dialog_title_delete_collection),
        confirmLabel = UiText.StringResource(R.string.action_delete),
        onConfirm = onConfirm,
        dismissLabel = UiText.StringResource(R.string.action_cancel),
    ) {
        Text(
            stringResource(R.string.dialog_message_delete_collection, collectionName),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@Composable
fun AddBooksDialog(
    allBooks: List<Book>,
    booksInCollection: List<Book>,
    onAddBook: (String) -> Unit,
    onDismiss: () -> Unit,
) {
    val collectionBookIds = remember(booksInCollection) { booksInCollection.map { it.id }.toSet() }
    val availableBooks = remember(allBooks, collectionBookIds) {
        allBooks.filter { it.id !in collectionBookIds }
    }

    LiberDialog(
        onDismissRequest = onDismiss,
        title = UiText.StringResource(R.string.dialog_title_add_books),
        confirmLabel = null,
        onConfirm = null,
        dismissLabel = UiText.StringResource(R.string.action_done),
    ) {
        if (availableBooks.isEmpty()) {
            Text(
                stringResource(R.string.dialog_message_all_books_added),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        } else {
            LazyColumn(modifier = Modifier.fillMaxWidth()) {
                items(availableBooks, key = { it.id }) { book ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                onAddBook(book.id)
                                onDismiss()
                            }
                            .padding(vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        BookCover(
                            book = book,
                            style = CoverStyle.SMALL,
                            isActive = false,
                            isPlaying = false,
                            modifier = Modifier
                                .size(if (book.isAudiobook) 36.dp else 36.dp, 54.dp),
                        )
                        Spacer(Modifier.width(12.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = book.title,
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurface,
                            )
                            book.author?.let {
                                Text(
                                    text = it,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
