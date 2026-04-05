package com.example.liber.ui.components

import android.net.Uri
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage

/**
 * Book cover image with a hardcover spine/lighting effect overlay.
 *
 * Two gradient styles:
 * - [CoverStyle.SMALL] — compact thumbnails (Continue / Previous cards)
 * - [CoverStyle.LARGE] — full-size covers (Want to Read, Library grid)
 *
 * The gradients recreate the left-spine highlight + shadow from the React reference.
 */
@Composable
fun BookCover(
    coverUri: Uri?,
    contentDescription: String,
    modifier: Modifier = Modifier,
    style: CoverStyle = CoverStyle.SMALL,
) {
    Box(modifier = modifier.clip(RoundedCornerShape(style.cornerRadius))) {
        AsyncImage(
            model = coverUri,
            contentDescription = contentDescription,
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.surfaceVariant),
            contentScale = ContentScale.Crop,
        )
        // Hardcover lighting overlay — mimics spine highlight + shadow
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(brush = style.gradient),
        )
    }
}

enum class CoverStyle(val cornerRadius: Dp, val gradient: Brush) {
    /**
     * Compact thumbnail (56 × 80 dp).
     * Matches: `bg-[linear-gradient(to_right,rgba(255,255,255,0.2)_0%,
     *            rgba(0,0,0,0.3)_4%,rgba(255,255,255,0.1)_6%,transparent_10%)]`
     */
    SMALL(
        cornerRadius = 6.dp,
        gradient = Brush.horizontalGradient(
            colorStops = arrayOf(
                0.00f to Color(0x33FFFFFF), // rgba(255,255,255, 0.20) — spine highlight
                0.04f to Color(0x4D000000), // rgba(0,0,0,       0.30) — spine shadow
                0.06f to Color(0x1AFFFFFF), // rgba(255,255,255, 0.10) — secondary highlight
                0.10f to Color.Transparent,
            ),
        ),
    ),

    /**
     * Full-size cover (e.g. 144 × 210 dp).
     * Matches: `bg-[linear-gradient(to_right,rgba(255,255,255,0.2)_0%,
     *            rgba(255,255,255,0)_1%,rgba(0,0,0,0.4)_3%,
     *            rgba(255,255,255,0.2)_4%,transparent_6%,
     *            transparent_98%,rgba(0,0,0,0.2)_100%)]`
     */
    LARGE(
        cornerRadius = 8.dp,
        gradient = Brush.horizontalGradient(
            colorStops = arrayOf(
                0.00f to Color(0x33FFFFFF), // rgba(255,255,255, 0.20) — leading highlight
                0.01f to Color(0x00FFFFFF), // fade out
                0.03f to Color(0x66000000), // rgba(0,0,0,       0.40) — spine shadow
                0.04f to Color(0x33FFFFFF), // rgba(255,255,255, 0.20) — secondary highlight
                0.06f to Color.Transparent,
                0.98f to Color.Transparent,
                1.00f to Color(0x33000000), // rgba(0,0,0,       0.20) — trailing edge shadow
            ),
        ),
    ),
}
