package com.example.liber.ui.components

import android.net.Uri
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.adamglin.PhosphorIcons
import com.adamglin.phosphoricons.Regular
import com.adamglin.phosphoricons.regular.Headphones
import com.example.liber.data.Book
import com.example.liber.ui.theme.Gambetta

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
    if (book.isAudiobook) {
        AudiobookCover(
            book = book,
            modifier = modifier,
            style = style,
            isActive = isActive,
            isPlaying = isPlaying,
        )
    } else {
        BookCover(
            coverUri = book.coverUri,
            contentDescription = book.title,
            modifier = modifier,
            style = style,
            fillBounds = fillBounds,
        )
    }
}

/**
 * Physical book cover image with a hardcover spine/lighting effect overlay.
 */
@Composable
fun BookCover(
    coverUri: Uri?,
    contentDescription: String,
    modifier: Modifier = Modifier,
    style: CoverStyle = CoverStyle.SMALL,
    fillBounds: Boolean = false,
) {
    Box(modifier = modifier) {
        AsyncImage(
            model = coverUri,
            contentDescription = contentDescription,
            modifier = if (fillBounds) Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.surfaceVariant)
            else Modifier
                .fillMaxWidth()
                .background(MaterialTheme.colorScheme.surfaceVariant),
            contentScale = if (fillBounds) ContentScale.Crop else ContentScale.FillWidth,
        )
        // Hardcover lighting overlay — mimics spine highlight + shadow
        Box(
            modifier = Modifier
                .matchParentSize()
                .background(brush = style.gradient),
        )
    }
}

/**
 * Vinyl-themed cover for audiobooks.
 */
@Composable
fun AudiobookCover(
    book: Book,
    modifier: Modifier = Modifier,
    style: CoverStyle = CoverStyle.SMALL,
    isActive: Boolean = false,
    isPlaying: Boolean = false,
) {
    val infiniteTransition = rememberInfiniteTransition(label = "vinyl")
    val rotation by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(3000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "vinyl_rotation"
    )

    val vinylOffset by animateDpAsState(
        targetValue = if (isActive && isPlaying) (-24).dp else 8.dp,
        animationSpec = tween(500),
        label = "vinyl_offset"
    )

    Box(
        modifier = modifier.aspectRatio(1f)
    ) {
        val isSmall = style == CoverStyle.SMALL
        val padding = if (isSmall) 4.dp else 8.dp
        val iconSize = if (isSmall) 10.dp else 16.dp
        val titleSize = if (isSmall) 10.sp else 14.sp

        // Vinyl Record behind the cover (Only for Large style)
        if (!isSmall) {
            Box(
                modifier = Modifier
                    .fillMaxSize(0.85f)
                    .align(Alignment.Center)
                    .offset(y = vinylOffset)
                    .rotate(if (isActive && isPlaying) rotation else 0f)
                    .clip(CircleShape)
                    .background(
                        Brush.sweepGradient(
                            listOf(
                                Color(0xFF111111),
                                Color(0xFF222222),
                                Color(0xFF111111)
                            )
                        )
                    )
                    .border(1.dp, Color.White.copy(alpha = 0.1f), CircleShape)
            ) {
                // Vinyl inner circle details
                Box(
                    modifier = Modifier
                        .fillMaxSize(0.33f)
                        .clip(CircleShape)
                        .background(Color(0xFF0A0A0A))
                        .border(1.dp, Color.White.copy(alpha = 0.1f), CircleShape)
                        .align(Alignment.Center)
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .fillMaxHeight(0.5f)
                            .align(Alignment.BottomCenter)
                            .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.5f))
                    )
                    Box(
                        modifier = Modifier
                            .size(4.dp)
                            .clip(CircleShape)
                            .background(Color(0xFF222222))
                            .align(Alignment.Center)
                    )
                }
            }
        }

        // Cover Image (Sleeve)
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(if (isActive && !isSmall) 0.dp else 4.dp)
                .shadow(
                    elevation = if (isActive) 12.dp else 4.dp,
                    shape = RoundedCornerShape(8.dp),
                    spotColor = if (isActive) Color.White.copy(alpha = 0.25f) else Color.Black
                )
                .clip(RoundedCornerShape(8.dp))
                .background(MaterialTheme.colorScheme.surfaceVariant)
                .then(
                    if (isActive) Modifier.border(
                        2.dp,
                        Color.White,
                        RoundedCornerShape(8.dp)
                    ) else Modifier.border(
                        1.dp,
                        Color.White.copy(alpha = 0.1f),
                        RoundedCornerShape(8.dp)
                    )
                )
        ) {
            AsyncImage(
                model = book.coverUri,
                contentDescription = "Cover of ${book.title}",
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize()
            )

            // Overlay for texture/lighting
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        Brush.verticalGradient(
                            listOf(Color.Transparent, Color.Black.copy(alpha = 0.4f)),
                            startY = 300f
                        )
                    )
            )

            // Icons and Text on cover (matching React mock)
            if (book.coverUri == null) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(padding),
                    verticalArrangement = Arrangement.SpaceBetween
                ) {
                    Icon(
                        imageVector = PhosphorIcons.Regular.Headphones,
                        contentDescription = null,
                        tint = Color.White.copy(alpha = 0.7f),
                        modifier = Modifier.size(iconSize)
                    )

                    Column {
                        Text(
                            text = book.title,
                            style = TextStyle(
                                fontFamily = Gambetta,
                                fontWeight = FontWeight.Medium,
                                fontSize = titleSize,
                                color = Color.White
                            ),
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis
                        )
                        if (!isSmall) {
                            Text(
                                text = book.author?.uppercase() ?: "UNKNOWN AUTHOR",
                                style = MaterialTheme.typography.labelSmall.copy(
                                    color = Color.White.copy(alpha = 0.7f),
                                    letterSpacing = 1.sp,
                                    fontSize = 8.sp
                                ),
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }
                }
            } else {
                // Just the headphones icon if there is a cover
                Icon(
                    imageVector = PhosphorIcons.Regular.Headphones,
                    contentDescription = null,
                    tint = Color.White.copy(alpha = 0.7f),
                    modifier = Modifier
                        .padding(padding)
                        .size(iconSize)
                )
            }
        }
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
