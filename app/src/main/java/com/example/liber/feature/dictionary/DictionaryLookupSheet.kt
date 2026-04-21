package com.example.liber.feature.dictionary

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.liber.R
import com.example.liber.core.designsystem.LiberModalBottomSheet
import com.example.liber.core.util.UiState
import com.example.liber.core.util.UiText
import com.example.liber.data.local.DictionaryEntryWithSenses

@Composable
fun DictionaryLookupSheet(
    query: String,
    lookupState: UiState<List<DictionaryEntryWithSenses>>,
    onDismiss: () -> Unit,
) {
    LiberModalBottomSheet(
        onDismissRequest = onDismiss,
        title = UiText.StringResource(R.string.reader_dictionary_sheet_title),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp)
                .padding(bottom = 40.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Text(
                text = query,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
            )

            when (lookupState) {
                is UiState.Loading -> {
                    CircularProgressIndicator()
                }

                is UiState.Error -> {
                    Text(
                        text = lookupState.message.asString(),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.error,
                    )
                }

                is UiState.Success -> {
                    if (lookupState.data.isEmpty()) {
                        Text(
                            text = stringResource(R.string.reader_dictionary_empty),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    } else {
                        LazyColumn(
                            modifier = Modifier.fillMaxWidth(),
                            verticalArrangement = Arrangement.spacedBy(10.dp),
                        ) {
                            items(lookupState.data, key = { it.entry.id }) { result ->
                                val primarySense = result.senses.firstOrNull()
                                Column(
                                    modifier = Modifier.fillMaxWidth(),
                                    verticalArrangement = Arrangement.spacedBy(4.dp),
                                ) {
                                    Text(
                                        text = result.entry.headword,
                                        style = MaterialTheme.typography.titleSmall,
                                        fontWeight = FontWeight.Medium,
                                    )
                                    Text(
                                        text = primarySense?.partOfSpeech
                                            ?: stringResource(R.string.reader_dictionary_unknown_pos),
                                        style = MaterialTheme.typography.labelMedium,
                                        color = MaterialTheme.colorScheme.primary,
                                    )
                                    Text(
                                        text = primarySense?.definition
                                            ?: stringResource(R.string.reader_dictionary_empty_definition),
                                        style = MaterialTheme.typography.bodyMedium,
                                    )
                                    primarySense?.example?.let { example ->
                                        Text(
                                            text = "\"$example\"",
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
    }
}
