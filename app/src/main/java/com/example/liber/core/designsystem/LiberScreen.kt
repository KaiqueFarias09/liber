package com.example.liber.core.designsystem

import androidx.compose.foundation.layout.Box
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.nestedscroll.NestedScrollConnection
import androidx.compose.ui.input.nestedscroll.NestedScrollSource
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.adamglin.PhosphorIcons
import com.adamglin.phosphoricons.Regular
import com.adamglin.phosphoricons.regular.ArrowLeft
import com.example.liber.R
import com.example.liber.core.util.UiText

/**
 * A standard screen layout for Liber app to ensure consistent spacing and header style.
 */
@Composable
fun LiberScreen(
    title: UiText?,
    modifier: Modifier = Modifier,
    onBack: (() -> Unit)? = null,
    navigationIcon: (@Composable () -> Unit)? = null,
    headerActions: (@Composable RowScope.() -> Unit)? = null,
    content: @Composable ColumnScope.() -> Unit
) {
    Column(
        modifier = modifier.fillMaxSize()
    ) {
        LiberHeader(
            title = title,
            onBack = onBack,
            navigationIcon = navigationIcon,
            actions = headerActions
        )
        content()
    }
}

/**
 * A scrollable version of [LiberScreen] using [LazyColumn].
 */
@Composable
fun LiberScrollableScreen(
    title: UiText?,
    modifier: Modifier = Modifier,
    onBack: (() -> Unit)? = null,
    navigationIcon: (@Composable () -> Unit)? = null,
    headerActions: (@Composable RowScope.() -> Unit)? = null,
    contentPadding: PaddingValues = PaddingValues(bottom = 24.dp),
    content: LazyListScope.() -> Unit
) {
    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = contentPadding
    ) {
        item {
            LiberHeader(
                title = title,
                onBack = onBack,
                navigationIcon = navigationIcon,
                actions = headerActions
            )
        }
        content()
    }
}

/**
 * A version of [LiberScreen] where the header collapses when scrolling content.
 */
@Composable
fun LiberCollapsingScreen(
    title: UiText?,
    modifier: Modifier = Modifier,
    onBack: (() -> Unit)? = null,
    navigationIcon: (@Composable () -> Unit)? = null,
    headerActions: (@Composable RowScope.() -> Unit)? = null,
    content: @Composable ColumnScope.() -> Unit
) {
    val density = LocalDensity.current
    val headerHeightState = remember { mutableIntStateOf(0) }
    val scrolledPxState = remember { mutableFloatStateOf(0f) }

    val nestedScrollConnection = remember {
        object : NestedScrollConnection {
            override fun onPreScroll(available: Offset, source: NestedScrollSource): Offset {
                val delta = available.y
                val maxScroll = headerHeightState.intValue.toFloat()
                val prevScrolled = scrolledPxState.floatValue
                val newScrolled = (prevScrolled - delta).coerceIn(0f, maxScroll)
                scrolledPxState.floatValue = newScrolled
                val consumed = prevScrolled - newScrolled
                return Offset(0f, consumed)
            }
        }
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .nestedScroll(nestedScrollConnection)
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            Spacer(
                Modifier
                    .fillMaxWidth()
                    .height(
                        with(density) {
                            (headerHeightState.intValue.toFloat() - scrolledPxState.floatValue)
                                .coerceAtLeast(0f).toDp()
                        }
                    )
            )
            content()
        }

        // Header overlay
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .onSizeChanged { headerHeightState.intValue = it.height }
                .graphicsLayer {
                    val scrolled = scrolledPxState.floatValue
                    val maxScroll = headerHeightState.intValue.toFloat().coerceAtLeast(1f)
                    translationY = -scrolled
                    alpha = (1f - scrolled / maxScroll).coerceIn(0f, 1f)
                }
        ) {
            LiberHeader(
                title = title,
                onBack = onBack,
                navigationIcon = navigationIcon,
                actions = headerActions
            )
        }
    }
}

@Composable
fun LiberHeader(
    title: UiText?,
    modifier: Modifier = Modifier,
    onBack: (() -> Unit)? = null,
    navigationIcon: (@Composable () -> Unit)? = null,
    actions: (@Composable RowScope.() -> Unit)? = null
) {
    // If absolutely nothing to show, just provide a standard top margin
    if (title == null && actions == null && navigationIcon == null && onBack == null) {
        Spacer(modifier = modifier.height(24.dp))
        return
    }

    val finalNavigationIcon: (@Composable () -> Unit)? = navigationIcon ?: if (onBack != null) {
        {
            IconButton(onClick = onBack) {
                Icon(
                    imageVector = PhosphorIcons.Regular.ArrowLeft,
                    contentDescription = stringResource(R.string.audio_control_back),
                )
            }
        }
    } else null

    Row(
        modifier = modifier
            .fillMaxWidth()
            .heightIn(min = 64.dp) // Standard Material-ish header height
            .padding(
                start = if (finalNavigationIcon != null) 4.dp else 24.dp,
                end = if (actions != null) 12.dp else 24.dp,
                top = 8.dp
            ),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (finalNavigationIcon != null) {
            finalNavigationIcon()
            // Minimal spacer because IconButton already has internal padding
            Spacer(modifier = Modifier.width(4.dp))
        }

        if (title != null) {
            Text(
                text = title.asString(),
                style = MaterialTheme.typography.headlineMedium,
                color = MaterialTheme.colorScheme.onBackground,
                modifier = Modifier
                    .weight(1f)
                    .padding(start = if (finalNavigationIcon == null) 0.dp else 4.dp)
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
