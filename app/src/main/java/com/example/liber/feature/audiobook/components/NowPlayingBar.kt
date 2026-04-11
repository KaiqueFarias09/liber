package com.example.liber.feature.audiobook.components

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
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
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.BlurredEdgeTreatment
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.adamglin.PhosphorIcons
import com.adamglin.phosphoricons.Fill
import com.adamglin.phosphoricons.Regular
import com.adamglin.phosphoricons.fill.Pause
import com.adamglin.phosphoricons.fill.Play
import com.adamglin.phosphoricons.regular.ArrowClockwise
import com.adamglin.phosphoricons.regular.ArrowCounterClockwise
import com.example.liber.data.model.Book

@Composable
fun NowPlayingBar(
    book: Book,
    isPlaying: Boolean,
    progress: Float,
    onTogglePlay: () -> Unit,
    modifier: Modifier = Modifier,
    onRewind: () -> Unit = {},
    onForward: () -> Unit = {},
    onClick: () -> Unit,
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

    Surface(
        modifier = modifier
            .padding(horizontal = 16.dp)
            .fillMaxWidth()
            .height(72.dp)
            .shadow(
                elevation = 20.dp,
                shape = RoundedCornerShape(20.dp),
                spotColor = Color.Black.copy(alpha = 0.3f)
            )
            .clip(RoundedCornerShape(20.dp))
            .clickable(onClick = onClick),
        color = Color(0xFF171717).copy(alpha = 0.95f),
        tonalElevation = 8.dp
    ) {
        Box(modifier = Modifier.fillMaxSize()) {
            // Subtle lighting effect
            Box(
                modifier = Modifier
                    .matchParentSize()
                    .background(
                        Brush.horizontalGradient(
                            colors = listOf(
                                MaterialTheme.colorScheme.primary.copy(alpha = 0.15f),
                                Color.Transparent
                            )
                        )
                    )
            )

            Column(modifier = Modifier.fillMaxSize()) {
                Row(
                    modifier = Modifier
                        .weight(1f)
                        .padding(start = 12.dp, end = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Sleeve + Vinyl Container
                    Box(modifier = Modifier.size(48.dp)) {
                        // Vinyl (sliding out)
                        Box(
                            modifier = Modifier
                                .size(44.dp)
                                .offset(x = 10.dp)
                                .align(Alignment.CenterStart)
                                .rotate(if (isPlaying) rotation else 0f)
                                .shadow(4.dp, CircleShape)
                                .background(
                                    Brush.sweepGradient(
                                        listOf(
                                            Color(0xFF111111),
                                            Color(0xFF222222),
                                            Color(0xFF111111)
                                        )
                                    ),
                                    CircleShape
                                )
                                .border(0.5.dp, Color(0xFF404040), CircleShape)
                        ) {
                            Box(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .padding(4.dp)
                                    .border(0.5.dp, Color.White.copy(alpha = 0.05f), CircleShape)
                            )
                            Box(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .padding(8.dp)
                                    .border(0.5.dp, Color.White.copy(alpha = 0.05f), CircleShape)
                            )
                            Box(
                                modifier = Modifier
                                    .size(16.dp)
                                    .clip(CircleShape)
                                    .background(Color.Black)
                                    .border(0.5.dp, Color(0xFF262626), CircleShape)
                                    .align(Alignment.Center)
                            ) {
                                AsyncImage(
                                    model = book.coverUri,
                                    contentDescription = null,
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .alpha(0.6f),
                                    contentScale = ContentScale.Crop
                                )
                                Box(
                                    modifier = Modifier
                                        .size(4.dp)
                                        .background(Color.White, CircleShape)
                                        .align(Alignment.Center)
                                )
                            }
                        }

                        // Square Sleeve (Top Layer)
                        AsyncImage(
                            model = book.coverUri,
                            contentDescription = null,
                            modifier = Modifier
                                .size(48.dp)
                                .shadow(8.dp, RoundedCornerShape(4.dp))
                                .clip(RoundedCornerShape(4.dp))
                                .border(
                                    0.5.dp,
                                    Color.White.copy(alpha = 0.1f),
                                    RoundedCornerShape(4.dp)
                                ),
                            contentScale = ContentScale.Crop
                        )
                    }

                    Spacer(modifier = Modifier.width(20.dp))

                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = book.title,
                            style = MaterialTheme.typography.titleSmall.copy(
                                fontWeight = FontWeight.Bold,
                                color = Color.White,
                                fontSize = 13.sp,
                                lineHeight = 16.sp
                            ),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Text(
                            text = book.author?.uppercase() ?: "UNKNOWN AUTHOR",
                            style = MaterialTheme.typography.labelSmall.copy(
                                color = Color(0xFFA3A3A3),
                                letterSpacing = 1.2.sp,
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Medium
                            ),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }

                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        IconButton(onClick = onRewind, modifier = Modifier.size(32.dp)) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    imageVector = PhosphorIcons.Regular.ArrowCounterClockwise,
                                    contentDescription = "Rewind 15s",
                                    tint = Color(0xFFA3A3A3),
                                    modifier = Modifier.size(20.dp)
                                )
                                Text(
                                    text = "15",
                                    style = MaterialTheme.typography.labelSmall.copy(
                                        fontSize = 7.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = Color(0xFFA3A3A3)
                                    ),
                                    modifier = Modifier.offset(y = 1.dp)
                                )
                            }
                        }

                        Surface(
                            onClick = onTogglePlay,
                            modifier = Modifier.size(40.dp),
                            shape = CircleShape,
                            color = Color.White,
                            contentColor = Color.Black,
                            shadowElevation = 4.dp
                        ) {
                            Box(
                                modifier = Modifier.fillMaxSize(),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = if (isPlaying) PhosphorIcons.Fill.Pause else PhosphorIcons.Fill.Play,
                                    contentDescription = if (isPlaying) "Pause" else "Play",
                                    modifier = Modifier
                                        .size(16.dp)
                                        .offset(x = if (!isPlaying) 1.dp else 0.dp)
                                )
                            }
                        }

                        IconButton(onClick = onForward, modifier = Modifier.size(32.dp)) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    imageVector = PhosphorIcons.Regular.ArrowClockwise,
                                    contentDescription = "Forward 15s",
                                    tint = Color(0xFFA3A3A3),
                                    modifier = Modifier.size(20.dp)
                                )
                                Text(
                                    text = "15",
                                    style = MaterialTheme.typography.labelSmall.copy(
                                        fontSize = 7.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = Color(0xFFA3A3A3)
                                    ),
                                    modifier = Modifier.offset(y = 1.dp)
                                )
                            }
                        }
                    }
                }

                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(4.dp)
                        .background(Color(0xFF262626).copy(alpha = 0.8f))
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth(progress.coerceIn(0f, 1f))
                            .fillMaxHeight()
                            .background(MaterialTheme.colorScheme.primary)
                    ) {
                        Box(
                            modifier = Modifier
                                .align(Alignment.CenterEnd)
                                .width(4.dp)
                                .fillMaxHeight()
                                .background(
                                    Brush.horizontalGradient(
                                        colors = listOf(
                                            Color.Transparent,
                                            Color.White.copy(alpha = 0.4f)
                                        )
                                    )
                                )
                                .blur(1.dp, BlurredEdgeTreatment.Unbounded)
                        )
                    }
                }
            }
        }
    }
}
