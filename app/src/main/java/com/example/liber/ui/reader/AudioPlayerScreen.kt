package com.example.liber.ui.reader

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
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
import com.adamglin.phosphoricons.regular.CaretDown
import com.adamglin.phosphoricons.regular.Clock
import com.adamglin.phosphoricons.regular.DotsThree
import com.adamglin.phosphoricons.regular.Headphones
import com.adamglin.phosphoricons.regular.List
import com.example.liber.data.Book
import org.readium.r2.shared.publication.Publication

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AudioPlayerScreen(
    book: Book,
    publication: Publication,
    liberAppViewModel: com.example.liber.ui.LiberAppViewModel,
    audiobookPlayerViewModel: AudiobookPlayerViewModel,
    onBack: () -> Unit,
    onSaveLocator: (String, Int) -> Unit
) {
    androidx.compose.runtime.LaunchedEffect(book) {
        audiobookPlayerViewModel.loadBook(book)
    }

    val isPlaying by audiobookPlayerViewModel.isPlaying.collectAsState()
    val positionMs by audiobookPlayerViewModel.positionMs.collectAsState()
    val durationMs by audiobookPlayerViewModel.durationMs.collectAsState()
    val currentTrackIndex by audiobookPlayerViewModel.currentTrackIndex.collectAsState()
    val tracks by audiobookPlayerViewModel.tracks.collectAsState()
    val isPrepared by audiobookPlayerViewModel.isPrepared.collectAsState()

    val globalIsPlaying by liberAppViewModel.isPlaying.collectAsState()

    androidx.compose.runtime.LaunchedEffect(isPlaying) {
        if (isPlaying != globalIsPlaying) {
            liberAppViewModel.setPlaying(isPlaying)
        }
    }

    androidx.compose.runtime.LaunchedEffect(positionMs, durationMs) {
        if (durationMs > 0) {
            liberAppViewModel.setPlayerProgress(positionMs.toFloat() / durationMs.toFloat())
        }
    }

    val infiniteTransition = rememberInfiniteTransition(label = "vinyl")
    val rotation by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(4000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "vinyl_rotation"
    )

    val vinylOffset by animateDpAsState(
        targetValue = if (isPlaying) (-80).dp else (-20).dp,
        animationSpec = tween(1000),
        label = "vinyl_offset"
    )

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(
                            PhosphorIcons.Regular.Headphones,
                            contentDescription = null,
                            modifier = Modifier.size(14.dp),
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            text = "NOW PLAYING",
                            style = MaterialTheme.typography.labelSmall.copy(
                                fontWeight = FontWeight.Bold,
                                letterSpacing = 2.sp
                            ),
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                },
                navigationIcon = {
                    IconButton(
                        onClick = onBack,
                        modifier = Modifier
                            .padding(start = 8.dp)
                            .size(40.dp)
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                    ) {
                        Icon(PhosphorIcons.Regular.CaretDown, contentDescription = "Back")
                    }
                },
                actions = {
                    IconButton(
                        onClick = { /* More actions */ },
                        modifier = Modifier
                            .padding(end = 8.dp)
                            .size(40.dp)
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                    ) {
                        Icon(PhosphorIcons.Regular.DotsThree, contentDescription = "More")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color.Transparent,
                    navigationIconContentColor = MaterialTheme.colorScheme.onBackground,
                    actionIconContentColor = MaterialTheme.colorScheme.onBackground
                )
            )
        },
        containerColor = Color(0xFF0A0A0A)
    ) { padding ->
        Box(modifier = Modifier.fillMaxSize()) {
            // Background Glow
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .fillMaxHeight(0.4f)
                    .background(
                        Brush.verticalGradient(
                            colors = listOf(
                                MaterialTheme.colorScheme.primary.copy(alpha = 0.12f),
                                Color.Transparent
                            )
                        )
                    )
            )

            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .padding(horizontal = 24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Spacer(modifier = Modifier.height(40.dp))

                // Vinyl Area - Flexible weight to allow breathing room
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth(),
                    contentAlignment = Alignment.Center
                ) {
                    // Vinyl Disk
                    Box(
                        modifier = Modifier
                            .offset(y = vinylOffset)
                            .fillMaxWidth(0.8f)
                            .aspectRatio(1f)
                            .graphicsLayer {
                                rotationZ = if (isPlaying) rotation else 0f
                            }
                            .clip(CircleShape)
                            .background(
                                Brush.sweepGradient(
                                    listOf(
                                        Color(0xFF050505),
                                        Color(0xFF1A1A1A),
                                        Color(0xFF050505)
                                    )
                                )
                            )
                            .border(1.dp, Color.White.copy(alpha = 0.05f), CircleShape)
                    ) {
                        // Grooves
                        repeat(6) { i ->
                            Box(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .padding((15 + i * 12).dp)
                                    .border(0.5.dp, Color.White.copy(alpha = 0.05f), CircleShape)
                            )
                        }

                        // Center Label
                        Box(
                            modifier = Modifier
                                .fillMaxWidth(0.35f)
                                .aspectRatio(1f)
                                .align(Alignment.Center)
                                .clip(CircleShape)
                                .background(Color(0xFF111111))
                                .border(1.dp, Color.White.copy(alpha = 0.1f), CircleShape)
                        ) {
                            AsyncImage(
                                model = book.coverUri,
                                contentDescription = null,
                                contentScale = ContentScale.Crop,
                                modifier = Modifier
                                    .fillMaxSize()
                                    .alpha(0.4f)
                            )
                            // Hole
                            Box(
                                modifier = Modifier
                                    .size(10.dp)
                                    .clip(CircleShape)
                                    .background(Color(0xFF0A0A0A))
                                    .align(Alignment.Center)
                                    .border(1.dp, Color.White.copy(alpha = 0.1f), CircleShape)
                            )
                        }
                    }

                    // Foreground Cover
                    Card(
                        modifier = Modifier
                            .fillMaxWidth(0.85f)
                            .aspectRatio(1f),
                        shape = RoundedCornerShape(12.dp),
                        elevation = CardDefaults.cardElevation(defaultElevation = 24.dp),
                        border = BorderStroke(1.dp, Color.White.copy(alpha = 0.12f))
                    ) {
                        Box {
                            AsyncImage(
                                model = book.coverUri,
                                contentDescription = "Cover",
                                contentScale = ContentScale.Crop,
                                modifier = Modifier.fillMaxSize()
                            )
                            // Plastic Reflection
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .fillMaxHeight(0.3f)
                                    .background(
                                        Brush.verticalGradient(
                                            listOf(
                                                Color.White.copy(alpha = 0.1f),
                                                Color.Transparent
                                            )
                                        )
                                    )
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(40.dp))

                // Metadata
                if (tracks.isNotEmpty()) {
                    Text(
                        text = tracks[currentTrackIndex].name.uppercase(),
                        style = MaterialTheme.typography.labelSmall.copy(
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 1.2.sp
                        ),
                        color = MaterialTheme.colorScheme.primary,
                        textAlign = TextAlign.Center,
                        maxLines = 1
                    )
                }
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = book.title,
                    style = MaterialTheme.typography.headlineSmall, // Gambetta
                    color = Color.White,
                    textAlign = TextAlign.Center,
                    maxLines = 2
                )
                Text(
                    text = book.author ?: "Unknown Author",
                    style = MaterialTheme.typography.bodyMedium, // Switzer
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center,
                    maxLines = 1
                )

                Spacer(modifier = Modifier.height(28.dp))

                // Progress
                Column {
                    Slider(
                        value = if (durationMs > 0) positionMs / durationMs.toFloat() else 0f,
                        onValueChange = { fraction ->
                            audiobookPlayerViewModel.seekTo((fraction * durationMs).toLong())
                        },
                        enabled = isPrepared,
                        modifier = Modifier.fillMaxWidth(),
                        colors = SliderDefaults.colors(
                            thumbColor = Color.White,
                            activeTrackColor = MaterialTheme.colorScheme.primary,
                            inactiveTrackColor = Color(0xFF1A1A1A)
                        )
                    )
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            formatTime(positionMs),
                            style = MaterialTheme.typography.labelSmall.copy(fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace),
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            "- ${formatTime(durationMs - positionMs)}",
                            style = MaterialTheme.typography.labelSmall.copy(fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace),
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                Spacer(modifier = Modifier.height(24.dp))

                // Main Controls
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceEvenly,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(onClick = { audiobookPlayerViewModel.skipBackward(15) }) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Icon(
                                PhosphorIcons.Regular.ArrowCounterClockwise,
                                contentDescription = "-15s",
                                tint = Color.LightGray,
                                modifier = Modifier.size(28.dp)
                            )
                            Text(
                                "-15s",
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold,
                                color = Color.LightGray
                            )
                        }
                    }

                    Spacer(modifier = Modifier.size(32.dp))

                    IconButton(
                        onClick = { audiobookPlayerViewModel.togglePlayPause() },
                        modifier = Modifier
                            .size(80.dp)
                            .clip(CircleShape)
                            .background(if (isPlaying) Color(0xFF1A1A1A) else Color.White)
                            .border(
                                1.dp,
                                if (isPlaying) Color(0xFF333333) else Color.Transparent,
                                CircleShape
                            )
                    ) {
                        Icon(
                            imageVector = if (isPlaying) PhosphorIcons.Fill.Pause else PhosphorIcons.Fill.Play,
                            contentDescription = if (isPlaying) "Pause" else "Play",
                            tint = if (isPlaying) Color.White else Color.Black,
                            modifier = Modifier.size(32.dp)
                        )
                    }

                    Spacer(modifier = Modifier.size(32.dp))

                    IconButton(onClick = { audiobookPlayerViewModel.skipForward(15) }) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Icon(
                                PhosphorIcons.Regular.ArrowClockwise,
                                contentDescription = "+15s",
                                tint = Color.LightGray,
                                modifier = Modifier.size(28.dp)
                            )
                            Text(
                                "+15s",
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold,
                                color = Color.LightGray
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(32.dp))

                // Bottom Controls
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .navigationBarsPadding()
                        .padding(bottom = 20.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(4.dp))
                            .background(Color(0xFF1A1A1A))
                            .border(1.dp, Color(0xFF333333), RoundedCornerShape(4.dp))
                            .padding(horizontal = 8.dp, vertical = 4.dp)
                    ) {
                        Text(
                            "1.25x",
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            color = Color.LightGray
                        )
                    }

                    Row(
                        modifier = Modifier
                            .clip(CircleShape)
                            .background(Color(0xFF1A1A1A).copy(alpha = 0.8f))
                            .border(1.dp, Color(0xFF333333), CircleShape)
                            .padding(horizontal = 16.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(
                            PhosphorIcons.Regular.List,
                            null,
                            Modifier.size(16.dp),
                            Color.LightGray
                        )
                        Text(
                            "Capítulos",
                            style = MaterialTheme.typography.labelMedium,
                            color = Color.LightGray
                        )
                    }

                    Icon(
                        PhosphorIcons.Regular.Clock,
                        contentDescription = "Sleep Timer",
                        tint = Color.Gray,
                        modifier = Modifier.size(24.dp)
                    )
                }
            }
        }
    }
}

private fun formatTime(ms: Long): String {
    val totalSeconds = ms / 1000
    val hours = totalSeconds / 3600
    val minutes = (totalSeconds % 3600) / 60
    val seconds = totalSeconds % 60
    return if (hours > 0) {
        "%d:%02d:%02d".format(hours, minutes, seconds)
    } else {
        "%d:%02d".format(minutes, seconds)
    }
}
