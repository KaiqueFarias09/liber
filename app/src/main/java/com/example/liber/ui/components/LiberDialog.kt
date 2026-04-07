package com.example.liber.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.BasicAlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.DialogProperties

/**
 * A standardized, premium-feeling dialog.
 *
 * Styling:
 * - Shape: 32dp rounded corners (Soft Material reference)
 * - Title: Gambetta (serif) via MaterialTheme.typography.headlineSmall
 * - Actions: [dismissLabel] in primary color, [confirmLabel] in onBackground (bold when active)
 *
 * @param onDismissRequest Called when the dialog is dismissed.
 * @param title The dialog title string.
 * @param confirmLabel The confirm button label. Set null to hide it.
 * @param onConfirm Called when the user taps the confirm button.
 * @param dismissLabel The dismiss/cancel button label. Set null to hide it.
 * @param onDismiss Called when the user taps the dismiss button. Defaults to [onDismissRequest].
 * @param confirmEnabled Whether the confirm button is active (e.g. form validation).
 * @param content Slot for the dialog body (description text, input field, list, etc.).
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LiberDialog(
    onDismissRequest: () -> Unit,
    title: String,
    modifier: Modifier = Modifier,
    confirmLabel: String? = "Save",
    onConfirm: (() -> Unit)? = null,
    dismissLabel: String? = "Cancel",
    onDismiss: (() -> Unit)? = null,
    confirmEnabled: Boolean = true,
    properties: DialogProperties = DialogProperties(),
    content: (@Composable () -> Unit)? = null,
) {
    BasicAlertDialog(
        onDismissRequest = onDismissRequest,
        properties = properties,
    ) {
        Surface(
            modifier = modifier.fillMaxWidth(),
            shape = RoundedCornerShape(32.dp),
            color = MaterialTheme.colorScheme.surfaceVariant,
            tonalElevation = 6.dp,
        ) {
            Column(modifier = Modifier.padding(24.dp)) {

                // Title — Gambetta serif
                Text(
                    text = title,
                    style = MaterialTheme.typography.headlineSmall,
                    color = MaterialTheme.colorScheme.onSurface,
                )

                if (content != null) {
                    Spacer(modifier = Modifier.height(20.dp))
                    content()
                }

                Spacer(modifier = Modifier.height(24.dp))

                // Actions row
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End,
                ) {
                    if (dismissLabel != null) {
                        TextButton(
                            onClick = { (onDismiss ?: onDismissRequest)() },
                        ) {
                            Text(
                                text = dismissLabel,
                                style = MaterialTheme.typography.labelLarge.copy(
                                    fontWeight = FontWeight.Medium,
                                ),
                                color = MaterialTheme.colorScheme.primary,
                            )
                        }
                    }
                    if (confirmLabel != null && onConfirm != null) {
                        TextButton(
                            onClick = onConfirm,
                            enabled = confirmEnabled,
                        ) {
                            Text(
                                text = confirmLabel,
                                style = MaterialTheme.typography.labelLarge.copy(
                                    fontWeight = FontWeight.SemiBold,
                                ),
                                color = if (confirmEnabled)
                                    MaterialTheme.colorScheme.onSurface
                                else
                                    MaterialTheme.colorScheme.onSurface.copy(alpha = 0.38f),
                            )
                        }
                    }
                }
            }
        }
    }
}
