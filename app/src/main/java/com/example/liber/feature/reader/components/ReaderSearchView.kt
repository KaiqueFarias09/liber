package com.example.liber.feature.reader.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.adamglin.PhosphorIcons
import com.adamglin.phosphoricons.Regular
import com.adamglin.phosphoricons.regular.ArrowDown
import com.adamglin.phosphoricons.regular.ArrowUp
import com.example.liber.R
import com.example.liber.core.designsystem.LiberSearchField
import com.example.liber.core.util.UiText
import com.example.liber.feature.reader.ReaderViewModel

@Composable
fun SearchView(
    viewModel: ReaderViewModel,
    modifier: Modifier = Modifier,
    onDismiss: () -> Unit,
) {
    val searchQuery by viewModel.searchQuery.collectAsState()
    val isSearching by viewModel.isSearching.collectAsState()

    Column(modifier = modifier.fillMaxSize()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            LiberSearchField(
                value = searchQuery,
                onValueChange = { viewModel.setSearchQuery(it) },
                modifier = Modifier.weight(1f),
                placeholder = UiText.StringResource(R.string.reader_search_placeholder),
                onClear = { viewModel.clearSearch() },
            )

            if (isSearching) {
                Box(modifier = Modifier.size(40.dp), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(20.dp),
                        color = MaterialTheme.colorScheme.primary,
                        strokeWidth = 2.dp,
                    )
                }
            } else {
                IconButton(
                    onClick = { viewModel.searchPrev(searchQuery) },
                    enabled = searchQuery.isNotBlank(),
                ) {
                    Icon(
                        PhosphorIcons.Regular.ArrowUp,
                        contentDescription = "Previous result",
                        tint = if (searchQuery.isBlank())
                            MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.38f)
                        else
                            MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                IconButton(
                    onClick = { viewModel.searchNext(searchQuery) },
                    enabled = searchQuery.isNotBlank(),
                ) {
                    Icon(
                        PhosphorIcons.Regular.ArrowDown,
                        contentDescription = "Next result",
                        tint = if (searchQuery.isBlank())
                            MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.38f)
                        else
                            MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        }
    }
}
