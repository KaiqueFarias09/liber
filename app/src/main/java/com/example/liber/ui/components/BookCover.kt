package com.example.liber.ui.components

import android.net.Uri
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import coil.compose.AsyncImage

/**
 * Book cover image with a hardcover spine/lighting effect overlay.
 *
 * Recreates the "hardcover" look with a spine highlight and shadow.
 * No border radius is applied here as per the latest requirements.
 */
@Composable
fun BookCover(
    coverUri: Uri?,
    contentDescription: String,
    modifier: Modifier = Modifier,
    style: CoverStyle = CoverStyle.SMALL,
) {
    Box(modifier = modifier) {
        AsyncImage(
            model = coverUri,
            contentDescription = contentDescription,
            modifier = Modifier
                .fillMaxWidth()
                .background(MaterialTheme.colorScheme.surfaceVariant),
            contentScale = ContentScale.FillWidth,
        )
        // Hardcover lighting overlay — mimics spine highlight + shadow
        // We use a simple Box with matchParentSize to ensure it overlays the image perfectly
        Box(
            modifier = Modifier
                .matchParentSize()
                .background(brush = style.gradient),
        )
    }
}

enum class CoverStyle(val gradient: Brush) {
    /**
     * Compact thumbnail.
     */
    SMALL(
        gradient = Brush.horizontalGradient(
            colorStops = arrayOf(
                0.00f to Color(0x66FFFFFF), // spine highlight (40%)
                0.04f to Color(0x66000000), // spine shadow (40%)
                0.06f to Color(0x33FFFFFF), // secondary highlight (20%)
                0.10f to Color.Transparent,
            ),
        ),
    ),

    /**
     * Full-size cover.
     */
    LARGE(
        gradient = Brush.horizontalGradient(
            colorStops = arrayOf(
                0.00f to Color(0x66FFFFFF), // leading highlight (40%)
                0.01f to Color(0x00FFFFFF), // fade out
                0.03f to Color(0x99000000), // spine shadow (60%)
                0.04f to Color(0x66FFFFFF), // secondary highlight (40%)
                0.06f to Color.Transparent,
                0.98f to Color.Transparent,
                1.00f to Color(0x66000000), // trailing edge shadow (40%)
            ),
        ),
    ),
}
