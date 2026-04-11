package com.example.liber.feature.reader.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import com.example.liber.R
import com.example.liber.core.designsystem.EmptyState
import com.example.liber.core.designsystem.LiberSearchField
import com.example.liber.feature.reader.ReaderViewModel
import org.readium.r2.shared.publication.Locator

@Composable
fun SearchView(
    viewModel: ReaderViewModel,
    modifier: Modifier = Modifier,
    onResultClick: (Locator) -> Unit,
) {
    val searchQuery by viewModel.searchQuery.collectAsState()
    val searchResults by viewModel.searchResults.collectAsState()
    val isSearching by viewModel.isSearching.collectAsState()

    Column(modifier = modifier.fillMaxSize()) {
        LiberSearchField(
            value = searchQuery,
            onValueChange = { viewModel.search(it) },
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 8.dp),
            placeholder = "Search in book…",
            onClear = { viewModel.search("") },
        )

        if (isSearching && searchResults.isEmpty()) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(32.dp)
                )
            }
        } else if (searchResults.isEmpty() && searchQuery.isNotBlank() && !isSearching) {
            EmptyState(
                title = "No results found",
                subtitle = "Try a different search term.",
                image = R.drawable.want_to_read_empty,
                showImage = false,
                showBackground = false,
                modifier = Modifier.padding(horizontal = 20.dp)
            )
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(bottom = 32.dp)
            ) {
                items(searchResults) { locator ->
                    SearchResultRow(locator = locator, onClick = { onResultClick(locator) })
                }
                item {
                    LaunchedEffect(Unit) { viewModel.loadNextResults() }
                }
            }
        }
    }
}

@Composable
private fun SearchResultRow(
    locator: Locator,
    onClick: () -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 20.dp, vertical = 12.dp)
    ) {
        locator.title?.let { title ->
            Text(
                text = title,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.primary,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(bottom = 4.dp)
            )
        }
        Text(
            text = buildAnnotatedString {
                val before = locator.text.before ?: ""
                if (before.length > 80) {
                    append("… ")
                    append(before.takeLast(77))
                } else {
                    append(before)
                }
                withStyle(SpanStyle(fontWeight = FontWeight.Bold)) {
                    append(locator.text.highlight ?: "")
                }
                append(locator.text.after ?: "")
            },
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            maxLines = 3,
            overflow = TextOverflow.Ellipsis
        )
    }
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(0.5.dp)
            .padding(horizontal = 20.dp)
            .background(MaterialTheme.colorScheme.outlineVariant)
    )
}
