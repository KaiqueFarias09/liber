package com.example.liber.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

/**
 * A standard screen layout for Liber app to ensure consistent spacing and header style.
 */
@Composable
fun LiberScreen(
    title: String?,
    modifier: Modifier = Modifier,
    headerActions: (@Composable RowScope.() -> Unit)? = null,
    content: @Composable ColumnScope.() -> Unit
) {
    Column(
        modifier = modifier.fillMaxSize()
    ) {
        LiberHeader(title = title, actions = headerActions)
        content()
    }
}

/**
 * A scrollable version of [LiberScreen] using [LazyColumn].
 */
@Composable
fun LiberScrollableScreen(
    title: String?,
    modifier: Modifier = Modifier,
    headerActions: (@Composable RowScope.() -> Unit)? = null,
    contentPadding: PaddingValues = PaddingValues(bottom = 24.dp),
    content: LazyListScope.() -> Unit
) {
    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = contentPadding
    ) {
        item {
            LiberHeader(title = title, actions = headerActions)
        }
        content()
    }
}

@Composable
fun LiberHeader(
    title: String?,
    modifier: Modifier = Modifier,
    actions: (@Composable RowScope.() -> Unit)? = null
) {
    // If absolutely nothing to show, just provide a standard top margin
    if (title.isNullOrEmpty() && actions == null) {
        Spacer(modifier = modifier.height(24.dp))
        return
    }

    Row(
        modifier = modifier
            .fillMaxWidth()
            .heightIn(min = 64.dp) // Standard Material-ish header height
            .padding(horizontal = 24.dp)
            .padding(top = 8.dp), // Slight top offset for status bar area breathing room
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (!title.isNullOrEmpty()) {
            Text(
                text = title,
                style = MaterialTheme.typography.headlineMedium,
                color = MaterialTheme.colorScheme.onBackground,
            )
        } else {
            // Keep a spacer if we have actions but no title
            Spacer(Modifier.weight(1f))
        }

        if (actions != null) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                content = actions
            )
        }
    }
}
