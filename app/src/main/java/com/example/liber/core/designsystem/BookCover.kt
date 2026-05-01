package com.example.liber.core.designsystem

import android.net.Uri
import androidx.compose.foundation.background
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.example.liber.data.model.Book
import com.example.liber.data.model.BookPreview

/**
 * Unified book cover component that automatically chooses between
 * the physical book "hardcover" look and the vinyl-themed audiobook look.
 */
@Composable
fun BookCover(
    book: Book,
    modifier: Modifier = Modifier,
    style: CoverStyle = CoverStyle.SMALL,
    fillBounds: Boolean = false,
    isActive: Boolean = false,
    isPlaying: Boolean = false,
) {
    BookCoverInternal(
        isAudiobook = book.isAudiobook,
        coverUri = book.coverUri,
        title = book.title,
        author = book.author,
        modifier = modifier,
        style = style,
        fillBounds = fillBounds,
        isActive = isActive,
        isPlaying = isPlaying,
    )
}

@Composable
fun BookCover(
    book: BookPreview,
    modifier: Modifier = Modifier,
    style: CoverStyle = CoverStyle.SMALL,
    fillBounds: Boolean = false,
    isActive: Boolean = false,
    isPlaying: Boolean = false,
) {
    BookCoverInternal(
        isAudiobook = book.isAudiobook,
        coverUri = book.coverUri,
        title = book.title,
        author = book.author,
        modifier = modifier,
        style = style,
        fillBounds = fillBounds,
        isActive = isActive,
        isPlaying = isPlaying,
    )
}

@Composable
private fun BookCoverInternal(
    isAudiobook: Boolean,
    coverUri: Uri?,
    title: String,
    author: String?,
    modifier: Modifier = Modifier,
    style: CoverStyle = CoverStyle.SMALL,
    fillBounds: Boolean = false,
    isActive: Boolean = false,
    isPlaying: Boolean = false,
) {
    if (isAudiobook) {
        AudiobookCover(
            coverUri = coverUri,
            title = title,
            author = author,
            modifier = modifier,
            style = style,
            isActive = isActive,
            isPlaying = isPlaying,
        )
    } else {
        BookCover(
            coverUri = coverUri,
            title = title,
            modifier = modifier,
            style = style,
            fillBounds = fillBounds,
        )
    }
}

/**
 * Physical book cover image with a hardcover spine/lighting effect overlay.
 * If coverUri is null, renders a generated fallback.
 */
@Composable
fun BookCover(
    coverUri: Uri?,
    title: String,
    modifier: Modifier = Modifier,
    style: CoverStyle = CoverStyle.SMALL,
    fillBounds: Boolean = false,
) {
    Box(
        modifier = modifier
            .then(
                if (fillBounds) {
                    Modifier.fillMaxSize()
                } else {
                    Modifier.fillMaxWidth()
                }
            )
            .background(MaterialTheme.colorScheme.surfaceVariant)
    ) {
        if (coverUri != null) {
            val context = LocalContext.current
            val request = remember(coverUri) {
                ImageRequest.Builder(context)
                    .data(coverUri)
                    .memoryCacheKey(coverUri.toString()) // Avoid Coil's FileKeyer doing disk I/O on main thread
                    .build()
            }
            AsyncImage(
                model = request,
                contentDescription = title,
                modifier = if (fillBounds) Modifier.fillMaxSize() else Modifier.fillMaxWidth(),
                contentScale = if (fillBounds) ContentScale.Crop else ContentScale.FillWidth,
            )
        } else {
            val colors = listOf(
                Color(0xFF5C6BC0), // Indigo 400
                Color(0xFF26A69A), // Teal 400
                Color(0xFF7E57C2), // Deep Purple 400
                Color(0xFF42A5F5), // Blue 400
                Color(0xFF9CCC65), // Light Green 400
                Color(0xFF8D6E63), // Brown 400
                Color(0xFF78909C), // Blue Grey 400
                Color(0xFFEC407A), // Pink 400
                Color(0xFFFF7043), // Deep Orange 400
                Color(0xFF26C6DA)  // Cyan 400
            )
            val bgColor = colors[kotlin.math.abs(title.hashCode()) % colors.size]

            Box(
                modifier = Modifier
                    .then(
                        if (fillBounds) Modifier.fillMaxSize() else Modifier
                            .fillMaxWidth()
                            .aspectRatio(2f / 3f)
                    )
                    .background(bgColor)
                    .padding(if (style == CoverStyle.SMALL) 8.dp else 16.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = title,
                    style = if (style == CoverStyle.SMALL)
                        MaterialTheme.typography.labelSmall
                    else
                        MaterialTheme.typography.headlineSmall,
                    color = Color.White,
                    textAlign = TextAlign.Center,
                    maxLines = 4,
                    overflow = TextOverflow.Ellipsis,
                    fontFamily = Gambetta
                )
            }
        }

        // Hardcover lighting overlay — mimics spine highlight + shadow
        Box(
            modifier = Modifier
                .matchParentSize()
                .background(brush = style.hardcoverGradient),
        )
    }
}


enum class CoverStyle {
    /**
     * Compact thumbnail.
     */
    SMALL,

    /**
     * Full-size cover.
     */
    LARGE;
}

val CoverStyle.gradient: Brush
    @Composable
    get() {
        val isDark = isSystemInDarkTheme()
        val highlightAlpha = if (isDark) 0.1f else 0.2f
        val shadowAlpha = if (isDark) 0.2f else 0.1f

        val highlight = Color.White.copy(alpha = highlightAlpha)
        val shadow = Color.Black.copy(alpha = shadowAlpha)

        return when (this) {
            CoverStyle.SMALL -> Brush.horizontalGradient(
                colorStops = arrayOf(
                    0.00f to highlight,
                    0.03f to shadow,
                    0.05f to Color.Transparent,
                ),
            )

            CoverStyle.LARGE -> Brush.horizontalGradient(
                colorStops = arrayOf(
                    0.00f to highlight,
                    0.01f to shadow,
                    0.02f to highlight.copy(alpha = highlightAlpha * 0.5f),
                    0.04f to Color.Transparent,
                ),
            )
        }
    }

val CoverStyle.hardcoverGradient: Brush
    @Composable
    get() {
        val isDark = isSystemInDarkTheme()
        val highlight = Color.White.copy(alpha = if (isDark) 0.3f else 0.5f)
        val shadow = Color.Black.copy(alpha = if (isDark) 0.5f else 0.3f)

        return when (this) {
            CoverStyle.SMALL -> Brush.horizontalGradient(
                colorStops = arrayOf(
                    0.00f to highlight,
                    0.04f to shadow,
                    0.06f to highlight.copy(alpha = highlight.alpha * 0.5f),
                    0.10f to Color.Transparent,
                ),
            )

            CoverStyle.LARGE -> Brush.horizontalGradient(
                colorStops = arrayOf(
                    0.00f to highlight,
                    0.01f to Color.Transparent,
                    0.03f to shadow.copy(alpha = shadow.alpha * 1.5f),
                    0.04f to highlight,
                    0.06f to Color.Transparent,
                    0.98f to Color.Transparent,
                    1.00f to shadow,
                ),
            )
        }
    }
