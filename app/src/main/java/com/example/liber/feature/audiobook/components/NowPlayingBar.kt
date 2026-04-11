package com.example.liber.feature.audiobook.components

import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.isSystemInDarkTheme
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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.BlurredEdgeTreatment
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.adamglin.PhosphorIcons
import com.adamglin.phosphoricons.Fill
import com.adamglin.phosphoricons.Regular
import com.adamglin.phosphoricons.fill.Pause
import com.adamglin.phosphoricons.fill.Play
import com.adamglin.phosphoricons.regular.ArrowClockwise
import com.adamglin.phosphoricons.regular.ArrowCounterClockwise
import com.example.liber.core.designsystem.AudiobookCover
import com.example.liber.core.designsystem.CoverStyle
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
    rememberInfiniteTransition(label = "vinyl")
    val isDark = isSystemInDarkTheme()

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
        color = MaterialTheme.colorScheme.surfaceContainerHigh.copy(alpha = 0.95f),
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
                    AudiobookCover(
                        book = book,
                        modifier = Modifier.size(54.dp),
                        style = CoverStyle.SMALL,
                        isActive = true,
                        isPlaying = isPlaying
                    )

                    Spacer(modifier = Modifier.width(20.dp))

                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = book.title,
                            style = MaterialTheme.typography.titleSmall.copy(
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface,
                                fontSize = 13.sp,
                                lineHeight = 16.sp
                            ),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Text(
                            text = book.author?.uppercase() ?: "UNKNOWN AUTHOR",
                            style = MaterialTheme.typography.labelSmall.copy(
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
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
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.size(20.dp)
                                )
                                Text(
                                    text = "15",
                                    style = MaterialTheme.typography.labelSmall.copy(
                                        fontSize = 7.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    ),
                                    modifier = Modifier.offset(y = 1.dp)
                                )
                            }
                        }

                        Surface(
                            onClick = onTogglePlay,
                            modifier = Modifier.size(40.dp),
                            shape = CircleShape,
                            color = MaterialTheme.colorScheme.primary,
                            contentColor = MaterialTheme.colorScheme.onPrimary,
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
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.size(20.dp)
                                )
                                Text(
                                    text = "15",
                                    style = MaterialTheme.typography.labelSmall.copy(
                                        fontSize = 7.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
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
                        .background(MaterialTheme.colorScheme.surfaceVariant)
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
                                            Color.White.copy(alpha = if (isDark) 0.4f else 0.7f)
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
