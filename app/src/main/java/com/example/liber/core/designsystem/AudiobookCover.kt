package com.example.liber.core.designsystem

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
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.adamglin.PhosphorIcons
import com.adamglin.phosphoricons.Regular
import com.adamglin.phosphoricons.regular.Headphones
import com.example.liber.data.model.Book
import kotlin.math.roundToInt

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
    AudiobookCover(
        coverUri = book.coverUri,
        title = book.title,
        author = book.author,
        modifier = modifier,
        style = style,
        isActive = isActive,
        isPlaying = isPlaying,
    )
}

@Composable
fun AudiobookCover(
    coverUri: Uri?,
    title: String,
    author: String?,
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
        targetValue = if (isActive && isPlaying) {
            if (style == CoverStyle.LARGE) (-80).dp else (-24).dp
        } else {
            if (style == CoverStyle.LARGE) (-20).dp else 8.dp
        },
        animationSpec = tween(if (style == CoverStyle.LARGE) 1000 else 500),
        label = "vinyl_offset"
    )

    Box(
        modifier = modifier.aspectRatio(1f)
    ) {
        val isSmall = style == CoverStyle.SMALL
        val padding = if (isSmall) 4.dp else 12.dp
        val iconSize = if (isSmall) 10.dp else 18.dp
        val titleSize = if (isSmall) 10.sp else 16.sp
        val isDark = isSystemInDarkTheme()

        // Vinyl Record behind the cover (Only for Large style)
        if (!isSmall) {
            Box(
                modifier = Modifier
                    .fillMaxSize(0.92f)
                    .align(Alignment.Center)
                    .offset { IntOffset(0, vinylOffset.value.roundToInt()) }
                    .rotate(if (isActive && isPlaying) rotation else 0f)
                    .clip(CircleShape)
                    .background(
                        Brush.sweepGradient(
                            0.0f to Color(0xFF050505),
                            0.25f to Color(0xFF1A1A1A),
                            0.5f to Color(0xFF050505),
                            0.75f to Color(0xFF1A1A1A),
                            1.0f to Color(0xFF050505)
                        )
                    )
                    .border(1.dp, Color.White.copy(alpha = 0.05f), CircleShape)
            ) {
                // Vinyl Grooves (multiple rings)
                val grooveColor = Color.White.copy(alpha = 0.05f)
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(2.dp)
                        .border(1.dp, grooveColor, CircleShape)
                )
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(5.dp)
                        .border(1.dp, grooveColor, CircleShape)
                )
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(8.dp)
                        .border(1.dp, grooveColor, CircleShape)
                )
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(12.dp)
                        .border(1.dp, grooveColor, CircleShape)
                )
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(24.dp)
                        .border(1.dp, grooveColor, CircleShape)
                )
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(32.dp)
                        .border(1.dp, grooveColor, CircleShape)
                )

                // Vinyl shine overlay
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(
                            Brush.verticalGradient(
                                listOf(Color.White.copy(alpha = 0.1f), Color.Transparent),
                                endY = 100f
                            )
                        )
                )

                // Vinyl Label
                Box(
                    modifier = Modifier
                        .fillMaxSize(0.38f)
                        .clip(CircleShape)
                        .background(Color(0xFF111111))
                        .border(1.dp, Color.White.copy(alpha = 0.1f), CircleShape)
                        .align(Alignment.Center)
                ) {
                    val context = LocalContext.current
                    val request = remember(coverUri) {
                        ImageRequest.Builder(context)
                            .data(coverUri)
                            .memoryCacheKey(coverUri?.toString()) // Avoid FileKeyer disk I/O on main thread
                            .build()
                    }
                    // Dimmed cover image on label
                    AsyncImage(
                        model = request,
                        contentDescription = null,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier
                            .fillMaxSize()
                            .alpha(0.4f)
                    )

                    // Label Overlays
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(Color.Black.copy(alpha = 0.5f))
                    )

                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(4.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            "SIDE A",
                            style = MaterialTheme.typography.labelSmall.copy(
                                fontSize = 5.sp,
                                letterSpacing = 1.sp,
                                color = Color.White.copy(alpha = 0.8f),
                                fontWeight = FontWeight.Bold
                            )
                        )

                        // Center hole
                        Box(
                            modifier = Modifier
                                .size(6.dp)
                                .clip(CircleShape)
                                .background(Color(0xFF0A0A0A))
                                .border(0.5.dp, Color.White.copy(alpha = 0.2f), CircleShape)
                        )

                        Text(
                            title.uppercase(),
                            style = MaterialTheme.typography.labelSmall.copy(
                                fontSize = 5.sp,
                                color = Color.White.copy(alpha = 0.8f),
                                fontWeight = FontWeight.Bold,
                                lineHeight = 6.sp
                            ),
                            maxLines = 2,
                            textAlign = TextAlign.Center
                        )
                    }
                }
            }
        }

        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.surfaceVariant)
                .then(
                    if (isActive) Modifier.border(
                        2.dp,
                        MaterialTheme.colorScheme.primary,
                    ) else Modifier.border(
                        1.dp,
                        Color.White.copy(alpha = 0.1f),
                    )
                )
        ) {
            val context = LocalContext.current
            val request = remember(coverUri) {
                ImageRequest.Builder(context)
                    .data(coverUri)
                    .memoryCacheKey(coverUri?.toString()) // Avoid FileKeyer disk I/O on main thread
                    .build()
            }
            AsyncImage(
                model = request,
                contentDescription = "Cover of $title",
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize()
            )

            // Overlay for texture/lighting (matching React mockup)
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .drawWithContent {
                        drawContent()
                        // Overlay blend simulation
                        drawRect(
                            color = Color.Black.copy(alpha = if (isDark) 0.1f else 0.05f),
                            blendMode = BlendMode.Overlay
                        )
                    }
                    .background(
                        Brush.linearGradient(
                            0.0f to Color.Black.copy(alpha = if (isDark) 0.6f else 0.4f),
                            0.6f to Color.Transparent,
                            start = Offset(0f, Float.POSITIVE_INFINITY),
                            end = Offset(Float.POSITIVE_INFINITY, 0f)
                        )
                    )
                    .background(
                        Brush.verticalGradient(
                            0.0f to Color.White.copy(alpha = if (isDark) 0.1f else 0.2f),
                            0.3f to Color.Transparent
                        )
                    )
                    .background(brush = style.gradient)
            )

            // Icons and Text on cover (matching React mock)
            if (coverUri == null) {
                val fallbackColor = MaterialTheme.colorScheme.primaryContainer
                val onFallbackColor = MaterialTheme.colorScheme.onPrimaryContainer

                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(fallbackColor)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(padding),
                        verticalArrangement = Arrangement.SpaceBetween
                    ) {
                        Icon(
                            imageVector = PhosphorIcons.Regular.Headphones,
                            contentDescription = null,
                            tint = onFallbackColor.copy(alpha = 0.7f),
                            modifier = Modifier.size(iconSize)
                        )

                        Column {
                            Text(
                                text = title,
                                style = TextStyle(
                                    fontFamily = Gambetta,
                                    fontWeight = FontWeight.Medium,
                                    fontSize = titleSize,
                                    color = onFallbackColor
                                ),
                                maxLines = 2,
                                overflow = TextOverflow.Ellipsis
                            )
                            if (!isSmall) {
                                Text(
                                    text = author?.uppercase() ?: "UNKNOWN AUTHOR",
                                    style = MaterialTheme.typography.labelSmall.copy(
                                        color = onFallbackColor.copy(alpha = 0.7f),
                                        letterSpacing = 1.sp,
                                        fontSize = 8.sp
                                    ),
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                        }
                    }
                }
            } else {
                // Just the headphones icon if there is a cover
                Icon(
                    imageVector = PhosphorIcons.Regular.Headphones,
                    contentDescription = null,
                    tint = Color.White.copy(alpha = 0.5f),
                    modifier = Modifier
                        .padding(padding)
                        .size(iconSize)
                )
            }
        }
    }
}
