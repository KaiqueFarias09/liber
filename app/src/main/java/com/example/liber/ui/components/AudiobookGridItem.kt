package com.example.liber.ui.components

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
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
import com.adamglin.phosphoricons.regular.PlayCircle
import com.example.liber.data.Book
import com.example.liber.ui.theme.Gambetta

@Composable
fun AudiobookGridItem(
    book: Book,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
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

    Column(
        modifier = modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(1f)
        ) {
            // Vinyl Record behind the cover
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
                    // This simulates the label on the vinyl
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

            // Cover Image (Sleeve)
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(if (isActive) 0.dp else 4.dp)
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
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(8.dp),
                    verticalArrangement = Arrangement.SpaceBetween
                ) {
                    Icon(
                        imageVector = PhosphorIcons.Regular.Headphones,
                        contentDescription = null,
                        tint = Color.White.copy(alpha = 0.7f),
                        modifier = Modifier.size(16.dp)
                    )

                    Column {
                        Text(
                            text = book.title,
                            style = TextStyle(
                                fontFamily = Gambetta,
                                fontWeight = FontWeight.Medium,
                                fontSize = 14.sp,
                                color = Color.White
                            ),
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis
                        )
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
        }

        Spacer(modifier = Modifier.height(12.dp))

        Text(
            text = book.title,
            style = MaterialTheme.typography.titleSmall.copy(
                fontWeight = FontWeight.SemiBold,
                color = if (isActive) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurfaceVariant
            ),
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.padding(horizontal = 4.dp)
        )

        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(horizontal = 4.dp)
        ) {
            Icon(
                imageVector = PhosphorIcons.Regular.PlayCircle,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                modifier = Modifier.size(12.dp)
            )
            Spacer(modifier = Modifier.width(4.dp))

            val remainingText = remember(book.durationMillis, book.readingProgress) {
                val duration = book.durationMillis ?: 0L
                if (duration <= 0L) {
                    if (book.readingProgress == 100) "Finished" else "Not started"
                } else {
                    val remainingMillis = duration * (100 - book.readingProgress) / 100
                    if (remainingMillis <= 0) {
                        "Finished"
                    } else {
                        val hours = remainingMillis / 3600000
                        val minutes = (remainingMillis % 3600000) / 60000
                        if (hours > 0) "${hours}h ${minutes}m left" else "${minutes}m left"
                    }
                }
            }

            Text(
                text = remainingText,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
            )
        }
    }
}
