package com.example.liber.feature.audiobook

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.BorderStroke
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
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
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
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
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
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.adamglin.PhosphorIcons
import com.adamglin.phosphoricons.Fill
import com.adamglin.phosphoricons.Regular
import com.adamglin.phosphoricons.fill.Check
import com.adamglin.phosphoricons.fill.Pause
import com.adamglin.phosphoricons.fill.Play
import com.adamglin.phosphoricons.regular.ArrowClockwise
import com.adamglin.phosphoricons.regular.ArrowCounterClockwise
import com.adamglin.phosphoricons.regular.CaretDown
import com.adamglin.phosphoricons.regular.Clock
import com.adamglin.phosphoricons.regular.DotsThree
import com.adamglin.phosphoricons.regular.List
import com.example.liber.core.designsystem.LiberDialog
import com.example.liber.core.designsystem.LiberModalBottomSheet
import com.example.liber.data.model.Book
import com.example.liber.feature.home.HomeViewModel
import com.example.liber.feature.home.components.BookDetailsBottomSheet
import kotlin.math.roundToInt

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AudioPlayerScreen(
    book: Book,
    liberAppViewModel: com.example.liber.ui.LiberAppViewModel,
    homeViewModel: HomeViewModel,
    audiobookPlayerViewModel: AudiobookPlayerViewModel,
    onBack: () -> Unit,
) {
    val isPlaying by audiobookPlayerViewModel.isPlaying.collectAsState()
    val playWhenReady by audiobookPlayerViewModel.playWhenReady.collectAsState()
    val positionMs by audiobookPlayerViewModel.positionMs.collectAsState()
    val durationMs by audiobookPlayerViewModel.durationMs.collectAsState()
    val currentTrackIndex by audiobookPlayerViewModel.currentTrackIndex.collectAsState()
    val tracks by audiobookPlayerViewModel.tracks.collectAsState()
    val isPrepared by audiobookPlayerViewModel.isPrepared.collectAsState()
    val playbackSpeed by audiobookPlayerViewModel.playbackSpeed.collectAsState()
    val sleepTimerRemainingMs by audiobookPlayerViewModel.sleepTimerRemainingMs.collectAsState()

    var activeSheet by remember { mutableStateOf<AudioPlayerSheet?>(null) }
    var showDetailsSheet by remember { mutableStateOf(false) }
    var showDeleteConfirmation by remember { mutableStateOf(false) }

    BackHandler {
        if (activeSheet != null) {
            activeSheet = null
        } else if (showDetailsSheet) {
            showDetailsSheet = false
        } else {
            onBack()
        }
    }

    androidx.compose.runtime.LaunchedEffect(book.id) {
        if (book.isAudiobook && (book.author == null || book.coverUri == null)) {
            val updatedBook = homeViewModel.bookImporter.fillAudiobookMetadata(book)
            if (updatedBook.author != book.author || updatedBook.coverUri != book.coverUri || updatedBook.title != book.title) {
                homeViewModel.updateFullMetadata(
                    book.id,
                    updatedBook.title,
                    updatedBook.author,
                    updatedBook.coverUri?.toString(),
                    book.narrator
                )
            }
        }
    }

    val globalIsPlaying by liberAppViewModel.isPlaying.collectAsState()

    var isDragging by remember { mutableStateOf(false) }
    var dragPosition by remember { mutableFloatStateOf(0f) }

    val currentSliderPosition = if (isDragging) dragPosition else {
        if (durationMs > 0) positionMs / durationMs.toFloat() else 0f
    }

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

    val infiniteTransition =
        androidx.compose.animation.core.rememberInfiniteTransition(label = "vinyl")
    val rotation by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = androidx.compose.animation.core.infiniteRepeatable(
            animation = androidx.compose.animation.core.tween(
                4000,
                easing = androidx.compose.animation.core.LinearEasing
            ),
            repeatMode = androidx.compose.animation.core.RepeatMode.Restart
        ),
        label = "vinyl_rotation"
    )

    val vinylOffset by androidx.compose.animation.core.animateDpAsState(
        targetValue = if (playWhenReady) (-80).dp else (-20).dp,
        animationSpec = androidx.compose.animation.core.tween(1000),
        label = "vinyl_offset"
    )

    if (showDeleteConfirmation) {
        LiberDialog(
            onDismissRequest = { showDeleteConfirmation = false },
            title = "Remove Download",
            confirmLabel = "Remove",
            confirmLabelColor = MaterialTheme.colorScheme.error,
            onConfirm = {
                homeViewModel.deleteBook(book.id)
                showDeleteConfirmation = false
                onBack()
            },
            dismissLabel = "Cancel"
        ) {
            Text(
                "Are you sure you want to remove this audiobook from your library? This action cannot be undone.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = {
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
                        onClick = { showDetailsSheet = true },
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
                    navigationIconContentColor = MaterialTheme.colorScheme.onSurface,
                    actionIconContentColor = MaterialTheme.colorScheme.onSurface
                )
            )
        },
        containerColor = MaterialTheme.colorScheme.background
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
                                MaterialTheme.colorScheme.primary.copy(alpha = 0.15f),
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
                    val coverToShow = remember(book.id, book.coverUri) { book.coverUri }

                    // Vinyl Disk
                    Box(
                        modifier = Modifier
                            .offset { IntOffset(0, vinylOffset.value.roundToInt()) }
                            .fillMaxWidth(0.8f)
                            .aspectRatio(1f)
                            .graphicsLayer {
                                rotationZ = if (playWhenReady) rotation else 0f
                            }
                            .clip(CircleShape)
                            .background(
                                Brush.sweepGradient(
                                    listOf(
                                        MaterialTheme.colorScheme.surface,
                                        MaterialTheme.colorScheme.onSurface.copy(alpha = 0.15f),
                                        MaterialTheme.colorScheme.surface
                                    )
                                )
                            )
                            .border(
                                1.dp,
                                MaterialTheme.colorScheme.onSurface.copy(alpha = 0.05f),
                                CircleShape
                            )
                    ) {
                        // Grooves
                        repeat(6) { i ->
                            Box(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .padding((15 + i * 12).dp)
                                    .border(
                                        0.5.dp,
                                        MaterialTheme.colorScheme.onSurface.copy(alpha = 0.05f),
                                        CircleShape
                                    )
                            )
                        }

                        // Center Label
                        Box(
                            modifier = Modifier
                                .fillMaxWidth(0.35f)
                                .aspectRatio(1f)
                                .align(Alignment.Center)
                                .clip(CircleShape)
                                .background(MaterialTheme.colorScheme.surfaceVariant)
                                .border(
                                    1.dp,
                                    MaterialTheme.colorScheme.onSurface.copy(alpha = 0.1f),
                                    CircleShape
                                )
                        ) {
                            AsyncImage(
                                model = coverToShow,
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
                                    .background(MaterialTheme.colorScheme.background)
                                    .align(Alignment.Center)
                                    .border(
                                        1.dp,
                                        MaterialTheme.colorScheme.onSurface.copy(alpha = 0.1f),
                                        CircleShape
                                    )
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
                        border = BorderStroke(
                            1.dp,
                            MaterialTheme.colorScheme.onSurface.copy(alpha = 0.1f)
                        )
                    ) {
                        Box {
                            AsyncImage(
                                model = coverToShow,
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
                    color = MaterialTheme.colorScheme.onSurface,
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
                        value = currentSliderPosition,
                        onValueChange = {
                            isDragging = true
                            dragPosition = it
                        },
                        onValueChangeFinished = {
                            audiobookPlayerViewModel.seekTo((dragPosition * durationMs).toLong())
                            isDragging = false
                        },
                        enabled = isPrepared,
                        modifier = Modifier.fillMaxWidth(),
                        colors = SliderDefaults.colors(
                            thumbColor = MaterialTheme.colorScheme.onSurface,
                            activeTrackColor = MaterialTheme.colorScheme.primary,
                            inactiveTrackColor = MaterialTheme.colorScheme.surfaceVariant
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
                                tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.8f),
                                modifier = Modifier.size(28.dp)
                            )
                            Text(
                                "-15s",
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.8f)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.size(32.dp))

                    IconButton(
                        onClick = { audiobookPlayerViewModel.togglePlayPause() },
                        modifier = Modifier
                            .size(80.dp)
                            .clip(CircleShape)
                            .background(if (playWhenReady) MaterialTheme.colorScheme.surfaceVariant else MaterialTheme.colorScheme.onSurface)
                            .border(
                                1.dp,
                                if (playWhenReady) MaterialTheme.colorScheme.outlineVariant else Color.Transparent,
                                CircleShape
                            )
                    ) {
                        Icon(
                            imageVector = if (playWhenReady) PhosphorIcons.Fill.Pause else PhosphorIcons.Fill.Play,
                            contentDescription = if (playWhenReady) "Pause" else "Play",
                            tint = if (playWhenReady) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.surface,
                            modifier = Modifier.size(32.dp)
                        )
                    }

                    Spacer(modifier = Modifier.size(32.dp))

                    IconButton(onClick = { audiobookPlayerViewModel.skipForward(15) }) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Icon(
                                PhosphorIcons.Regular.ArrowClockwise,
                                contentDescription = "+15s",
                                tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.8f),
                                modifier = Modifier.size(28.dp)
                            )
                            Text(
                                "+15s",
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.8f)
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
                            .clip(RoundedCornerShape(6.dp))
                            .background(MaterialTheme.colorScheme.surfaceContainerLow)
                            .clickable { activeSheet = AudioPlayerSheet.SPEED }
                            .padding(horizontal = 10.dp, vertical = 6.dp)
                    ) {
                        Text(
                            "${playbackSpeed}x",
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }

                    Row(
                        modifier = Modifier
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.surfaceContainerLow)
                            .clickable { activeSheet = AudioPlayerSheet.CHAPTERS }
                            .padding(horizontal = 16.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(
                            PhosphorIcons.Regular.List,
                            null,
                            Modifier.size(18.dp),
                            MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            "Chapters",
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.Medium,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }

                    IconButton(onClick = { activeSheet = AudioPlayerSheet.SLEEP }) {
                        Box {
                            Icon(
                                PhosphorIcons.Regular.Clock,
                                contentDescription = "Sleep Timer",
                                tint = if (sleepTimerRemainingMs != null) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.size(24.dp)
                            )
                            if (sleepTimerRemainingMs != null) {
                                Box(
                                    modifier = Modifier
                                        .size(8.dp)
                                        .clip(CircleShape)
                                        .background(MaterialTheme.colorScheme.primary)
                                        .align(Alignment.TopEnd)
                                        .border(
                                            1.5.dp,
                                            MaterialTheme.colorScheme.background,
                                            CircleShape
                                        )
                                )
                            }
                        }
                    }
                }
            }
        }

        if (showDetailsSheet) {
            BookDetailsBottomSheet(
                book = book,
                homeViewModel = homeViewModel,
                onDismiss = { showDetailsSheet = false },
                onDelete = { showDeleteConfirmation = true },
                onShare = { /* Not implemented yet */ }
            )
        }

        activeSheet?.let { sheet ->
            LiberModalBottomSheet(
                onDismissRequest = { activeSheet = null },
                title = when (sheet) {
                    AudioPlayerSheet.SPEED -> "Playback Speed"
                    AudioPlayerSheet.CHAPTERS -> "Chapters"
                    AudioPlayerSheet.SLEEP -> "Sleep Timer"
                }
            ) {
                AnimatedContent(
                    targetState = sheet,
                    transitionSpec = {
                        slideInHorizontally { it } togetherWith slideOutHorizontally { -it }
                    },
                    label = "sheet_transition"
                ) { targetSheet ->
                    when (targetSheet) {
                        AudioPlayerSheet.SPEED -> SpeedSheet(
                            currentSpeed = playbackSpeed,
                            onSpeedSelected = {
                                audiobookPlayerViewModel.setPlaybackSpeed(it)
                                activeSheet = null
                            }
                        )

                        AudioPlayerSheet.CHAPTERS -> ChaptersSheet(
                            tracks = tracks,
                            currentIndex = currentTrackIndex,
                            onTrackSelected = { index ->
                                audiobookPlayerViewModel.playTrack(index)
                                activeSheet = null
                            }
                        )

                        AudioPlayerSheet.SLEEP -> SleepTimerSheet(
                            remainingMs = sleepTimerRemainingMs,
                            onTimerSelected = { minutes ->
                                audiobookPlayerViewModel.setSleepTimer(minutes)
                                activeSheet = null
                            },
                            onEndOfChapterSelected = {
                                audiobookPlayerViewModel.setSleepTimerEndOfChapter()
                                activeSheet = null
                            }
                        )
                    }
                }
            }
        }
    }
}

enum class AudioPlayerSheet {
    SPEED, CHAPTERS, SLEEP
}

@Composable
fun SpeedSheet(currentSpeed: Float, onSpeedSelected: (Float) -> Unit) {
    Column(
        modifier = Modifier
            .padding(horizontal = 20.dp)
            .padding(bottom = 40.dp)
            .padding(top = 8.dp)
    ) {
        val speeds = listOf(0.8f, 1.0f, 1.25f, 1.5f, 1.75f, 2.0f)
        androidx.compose.foundation.layout.FlowRow(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
            maxItemsInEachRow = 3
        ) {
            speeds.forEach { speed ->
                val isSelected = speed == currentSpeed
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(12.dp))
                        .background(if (isSelected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceContainerLow)
                        .clickable { onSpeedSelected(speed) }
                        .padding(vertical = 12.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        "${speed}x",
                        color = if (isSelected) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurface,
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Medium
                    )
                }
            }
        }
    }
}

@Composable
fun ChaptersSheet(
    tracks: List<AudiobookPlayerViewModel.TrackInfo>,
    currentIndex: Int,
    onTrackSelected: (Int) -> Unit
) {
    Column(
        modifier = Modifier
            .padding(horizontal = 20.dp)
            .padding(bottom = 40.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 12.dp),
            horizontalArrangement = Arrangement.End,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                "${tracks.size} chapters",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }

        LazyColumn(
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            itemsIndexed(tracks) { index, track ->
                val isActive = index == currentIndex
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .background(if (isActive) MaterialTheme.colorScheme.primaryContainer else Color.Transparent)
                        .clickable { onTrackSelected(index) }
                        .padding(horizontal = 16.dp, vertical = 12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    if (isActive) {
                        Icon(
                            PhosphorIcons.Fill.Play,
                            contentDescription = null,
                            modifier = Modifier.size(14.dp),
                            tint = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                    } else {
                        Text(
                            "${index + 1}",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                            modifier = Modifier.width(16.dp),
                            textAlign = TextAlign.Center
                        )
                    }

                    Text(
                        track.name,
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = if (isActive) FontWeight.SemiBold else FontWeight.Medium,
                        color = if (isActive) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier.weight(1f),
                        maxLines = 1
                    )
                }
            }
        }
    }
}

@Composable
fun SleepTimerSheet(
    remainingMs: Long?,
    onTimerSelected: (Int?) -> Unit,
    onEndOfChapterSelected: () -> Unit
) {
    Column(
        modifier = Modifier
            .padding(horizontal = 20.dp)
            .padding(bottom = 40.dp)
            .padding(top = 8.dp)
    ) {
        val options = listOf(
            "Off" to null,
            "15 Minutos" to 15,
            "30 Minutos" to 30,
            "45 Minutos" to 45
        )

        options.forEach { (label, minutes) ->
            val isSelected = if (minutes == null) remainingMs == null else {
                remainingMs != null && remainingMs != -1L && (remainingMs / 60000.0).toInt() <= minutes && (remainingMs / 60000.0).toInt() > (minutes - 15).coerceAtLeast(
                    0
                )
            }

            SleepTimerOption(
                label = label,
                isSelected = isSelected,
                onClick = { onTimerSelected(minutes) }
            )
        }

        SleepTimerOption(
            label = "Fim do Capítulo",
            isSelected = remainingMs == -1L,
            onClick = onEndOfChapterSelected
        )

        Text(
            "A reprodução será pausada automaticamente.",
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 24.dp),
            textAlign = TextAlign.Center
        )
    }
}

@Composable
fun SleepTimerOption(label: String, isSelected: Boolean, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(if (isSelected) MaterialTheme.colorScheme.primaryContainer else Color.Transparent)
            .clickable { onClick() }
            .padding(horizontal = 16.dp, vertical = 12.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            label,
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Medium,
            color = if (isSelected) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurface
        )
        if (isSelected) {
            Icon(
                PhosphorIcons.Fill.Check,
                contentDescription = null,
                modifier = Modifier.size(18.dp),
                tint = MaterialTheme.colorScheme.onPrimaryContainer
            )
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
