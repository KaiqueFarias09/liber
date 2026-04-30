package com.example.liber.feature.audiobook

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.adamglin.PhosphorIcons
import com.adamglin.phosphoricons.Fill
import com.adamglin.phosphoricons.Regular
import com.adamglin.phosphoricons.fill.Check
import com.adamglin.phosphoricons.fill.Pause
import com.adamglin.phosphoricons.fill.Play
import com.adamglin.phosphoricons.regular.ArrowClockwise
import com.adamglin.phosphoricons.regular.ArrowCounterClockwise
import com.adamglin.phosphoricons.regular.ArrowLeft
import com.adamglin.phosphoricons.regular.Clock
import com.adamglin.phosphoricons.regular.DotsThree
import com.adamglin.phosphoricons.regular.List
import com.adamglin.phosphoricons.regular.SlidersHorizontal
import com.example.liber.R
import com.example.liber.core.designsystem.AudiobookCover
import com.example.liber.core.designsystem.CoverStyle
import com.example.liber.core.designsystem.Gambetta
import com.example.liber.core.designsystem.LiberDialog
import com.example.liber.core.designsystem.LiberModalBottomSheet
import com.example.liber.core.designsystem.liberHorizontalDivider
import com.example.liber.core.designsystem.responsiveMaxWidth
import com.example.liber.core.util.UiText
import com.example.liber.core.util.toFormattedPlaybackTime
import com.example.liber.data.model.Book
import com.example.liber.feature.home.HomeViewModel
import com.example.liber.feature.home.components.BookDetailsBottomSheet

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

    BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
        val isTabletLandscape = maxWidth >= 720.dp && maxWidth > maxHeight

        BackHandler {
            if (!isTabletLandscape && activeSheet != null) {
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

        androidx.compose.animation.core.rememberInfiniteTransition(label = "vinyl")

        if (showDeleteConfirmation) {
            LiberDialog(
                onDismissRequest = { showDeleteConfirmation = false },
                title = UiText.StringResource(R.string.dialog_title_remove_download),
                confirmLabel = UiText.StringResource(R.string.action_remove),
                confirmLabelColor = MaterialTheme.colorScheme.error,
                onConfirm = {
                    homeViewModel.deleteBook(book.id)
                    showDeleteConfirmation = false
                    onBack()
                },
                dismissLabel = UiText.StringResource(R.string.action_cancel)
            ) {
                Text(
                    stringResource(R.string.dialog_message_remove_download),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        if (isTabletLandscape) {
            AudioPlayerTabletLandscape(
                book = book,
                playWhenReady = playWhenReady,
                positionMs = positionMs,
                durationMs = durationMs,
                currentTrackIndex = currentTrackIndex,
                tracks = tracks,
                playbackSpeed = playbackSpeed,
                sleepTimerRemainingMs = sleepTimerRemainingMs,
                audiobookPlayerViewModel = audiobookPlayerViewModel,
                onBack = onBack,
                onShowDetails = { showDetailsSheet = true }
            )
        } else {
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
                                    .background(MaterialTheme.colorScheme.surfaceContainerLowest)
                                    .border(
                                        1.dp,
                                        MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f),
                                        CircleShape
                                    )
                            ) {
                                Icon(
                                    imageVector = PhosphorIcons.Regular.ArrowLeft,
                                    contentDescription = stringResource(R.string.audio_control_back)
                                )
                            }
                        },
                        actions = {
                            IconButton(
                                onClick = { showDetailsSheet = true },
                                modifier = Modifier
                                    .padding(end = 8.dp)
                                    .size(40.dp)
                                    .clip(CircleShape)
                                    .background(MaterialTheme.colorScheme.surfaceContainerLowest)
                                    .border(
                                        1.dp,
                                        MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f),
                                        CircleShape
                                    )
                            ) {
                                Icon(
                                    imageVector = PhosphorIcons.Regular.DotsThree,
                                    contentDescription = stringResource(R.string.audio_control_more)
                                )
                            }
                        },
                        colors = TopAppBarDefaults.topAppBarColors(
                            containerColor = Color.Transparent,
                            navigationIconContentColor = MaterialTheme.colorScheme.onSurface,
                            actionIconContentColor = MaterialTheme.colorScheme.onSurface
                        ),
                        modifier = Modifier.fillMaxWidth()
                    )
                },
                containerColor = MaterialTheme.colorScheme.background
            ) { padding ->
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.TopCenter
                ) {
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
                            .fillMaxHeight()
                            .responsiveMaxWidth()
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
                            AudiobookCover(
                                book = book,
                                modifier = Modifier
                                    .fillMaxWidth(0.95f)
                                    .aspectRatio(1f),
                                style = CoverStyle.LARGE,
                                isActive = true,
                                isPlaying = playWhenReady
                            )
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
                            text = book.author ?: stringResource(R.string.label_unknown_author),
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
                                    positionMs.toFormattedPlaybackTime(),
                                    style = MaterialTheme.typography.labelSmall.copy(fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace),
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Text(
                                    "- ${(durationMs - positionMs).toFormattedPlaybackTime()}",
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
                                        imageVector = PhosphorIcons.Regular.ArrowCounterClockwise,
                                        contentDescription = stringResource(R.string.audio_control_skip_backward),
                                        tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.8f),
                                        modifier = Modifier.size(28.dp)
                                    )
                                    Text(
                                        text = stringResource(R.string.audio_label_skip_backward_short),
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
                                    .background(if (playWhenReady) MaterialTheme.colorScheme.surfaceContainerLowest else MaterialTheme.colorScheme.onSurface)
                                    .border(
                                        1.dp,
                                        if (playWhenReady) MaterialTheme.colorScheme.outlineVariant.copy(
                                            alpha = 0.4f
                                        ) else Color.Transparent,
                                        CircleShape
                                    )
                            ) {
                                Icon(
                                    imageVector = if (playWhenReady) PhosphorIcons.Fill.Pause else PhosphorIcons.Fill.Play,
                                    contentDescription = if (playWhenReady) stringResource(R.string.audio_control_pause) else stringResource(
                                        R.string.audio_control_play
                                    ),
                                    tint = if (playWhenReady) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.surface,
                                    modifier = Modifier.size(32.dp)
                                )
                            }

                            Spacer(modifier = Modifier.size(32.dp))

                            IconButton(onClick = { audiobookPlayerViewModel.skipForward(15) }) {
                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    Icon(
                                        imageVector = PhosphorIcons.Regular.ArrowClockwise,
                                        contentDescription = stringResource(R.string.audio_control_skip_forward),
                                        tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.8f),
                                        modifier = Modifier.size(28.dp)
                                    )
                                    Text(
                                        text = stringResource(R.string.audio_label_skip_forward_short),
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
                                    .background(MaterialTheme.colorScheme.surfaceContainerLowest)
                                    .border(
                                        1.dp,
                                        MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f),
                                        RoundedCornerShape(6.dp)
                                    )
                                    .clickable { activeSheet = AudioPlayerSheet.SPEED }
                                    .padding(horizontal = 10.dp, vertical = 6.dp)
                            ) {
                                Text(
                                    text = stringResource(
                                        R.string.audio_label_speed,
                                        playbackSpeed
                                    ),
                                    style = MaterialTheme.typography.labelMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                            }

                            Row(
                                modifier = Modifier
                                    .clip(CircleShape)
                                    .background(MaterialTheme.colorScheme.surfaceContainerLowest)
                                    .border(
                                        1.dp,
                                        MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f),
                                        CircleShape
                                    )
                                    .clickable { activeSheet = AudioPlayerSheet.CHAPTERS }
                                    .padding(horizontal = 16.dp, vertical = 8.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Icon(
                                    imageVector = PhosphorIcons.Regular.List,
                                    contentDescription = null,
                                    modifier = Modifier.size(18.dp),
                                    tint = MaterialTheme.colorScheme.onSurface
                                )
                                Text(
                                    text = stringResource(R.string.audio_label_chapters),
                                    style = MaterialTheme.typography.labelMedium,
                                    fontWeight = FontWeight.Medium,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                            }

                            IconButton(onClick = { activeSheet = AudioPlayerSheet.SLEEP }) {
                                Box {
                                    Icon(
                                        PhosphorIcons.Regular.Clock,
                                        contentDescription = stringResource(R.string.audio_sleep_timer),
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

                activeSheet?.let { sheet ->
                    LiberModalBottomSheet(
                        onDismissRequest = { activeSheet = null },
                        title = when (sheet) {
                            AudioPlayerSheet.SPEED -> UiText.StringResource(R.string.audio_playback_speed)
                            AudioPlayerSheet.CHAPTERS -> UiText.StringResource(R.string.audio_chapters)
                            AudioPlayerSheet.SLEEP -> UiText.StringResource(R.string.audio_sleep_timer)
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

        if (showDetailsSheet) {
            BookDetailsBottomSheet(
                book = book,
                homeViewModel = homeViewModel,
                onDismiss = { showDetailsSheet = false },
                onDelete = { showDeleteConfirmation = true },
                onShare = { /* Not implemented yet */ }
            )
        }
    }
}

@Composable
private fun AudioPlayerTabletLandscape(
    book: Book,
    playWhenReady: Boolean,
    positionMs: Long,
    durationMs: Long,
    currentTrackIndex: Int,
    tracks: List<AudiobookPlayerViewModel.TrackInfo>,
    playbackSpeed: Float,
    sleepTimerRemainingMs: Long?,
    audiobookPlayerViewModel: AudiobookPlayerViewModel,
    onBack: () -> Unit,
    onShowDetails: () -> Unit,
) {
    var activePanel by remember { mutableStateOf(AudioPlayerSheet.CHAPTERS) }

    Row(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        // LEFT PANE (40%)
        Column(
            modifier = Modifier
                .weight(0.4f)
                .fillMaxHeight()
                .background(MaterialTheme.colorScheme.surfaceContainerLowest)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 24.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(
                    onClick = onBack,
                    modifier = Modifier
                        .size(44.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.surface.copy(alpha = 0.5f))
                ) {
                    Icon(
                        imageVector = PhosphorIcons.Regular.ArrowLeft,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSurface
                    )
                }
                IconButton(
                    onClick = onShowDetails,
                    modifier = Modifier
                        .size(44.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.surface.copy(alpha = 0.5f))
                ) {
                    Icon(
                        imageVector = PhosphorIcons.Regular.DotsThree,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSurface
                    )
                }
            }

            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 48.dp, vertical = 24.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                AudiobookCover(
                    book = book,
                    modifier = Modifier
                        .fillMaxWidth(0.85f)
                        .aspectRatio(1f)
                        .shadow(32.dp, shape = RoundedCornerShape(4.dp)),
                    style = CoverStyle.LARGE,
                    isActive = true,
                    isPlaying = playWhenReady
                )

                Spacer(modifier = Modifier.height(48.dp))

                Text(
                    text = book.title,
                    style = MaterialTheme.typography.displaySmall.copy(fontFamily = Gambetta),
                    textAlign = TextAlign.Center,
                    color = MaterialTheme.colorScheme.onSurface
                )

                Spacer(modifier = Modifier.height(8.dp))

                Text(
                    text = (book.author
                        ?: stringResource(R.string.label_unknown_author)).uppercase(),
                    style = MaterialTheme.typography.labelSmall.copy(
                        letterSpacing = 2.sp,
                        fontWeight = FontWeight.Bold
                    ),
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
                    textAlign = TextAlign.Center
                )
            }
        }

        // Divider
        Box(
            modifier = Modifier
                .fillMaxHeight()
                .width(1.dp)
                .background(MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.2f))
        )

        // RIGHT PANE (60%)
        Column(
            modifier = Modifier
                .weight(0.6f)
                .fillMaxHeight()
        ) {
            // TOP HALF: Contextual Panel
            Column(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .padding(start = 64.dp, end = 64.dp, top = 32.dp, bottom = 12.dp)
            ) {
                // Panel Header
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 24.dp)
                        .liberHorizontalDivider(
                            color = MaterialTheme.colorScheme.outlineVariant.copy(
                                alpha = 0.2f
                            )
                        ),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.Bottom
                ) {
                    Text(
                        text = when (activePanel) {
                            AudioPlayerSheet.CHAPTERS -> stringResource(R.string.audio_chapters)
                            AudioPlayerSheet.SPEED -> stringResource(R.string.audio_playback_speed)
                            AudioPlayerSheet.SLEEP -> stringResource(R.string.audio_sleep_timer)
                        },
                        style = MaterialTheme.typography.headlineLarge.copy(fontFamily = Gambetta),
                        modifier = Modifier.padding(bottom = 12.dp)
                    )

                    if (activePanel == AudioPlayerSheet.CHAPTERS) {
                        Text(
                            text = pluralStringResource(
                                R.plurals.audio_chapters_count,
                                tracks.size,
                                tracks.size
                            ).uppercase(),
                            style = MaterialTheme.typography.labelSmall.copy(
                                letterSpacing = 1.sp,
                                fontWeight = FontWeight.Bold
                            ),
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f),
                            modifier = Modifier.padding(bottom = 12.dp)
                        )
                    }
                }

                // Panel Content
                Box(modifier = Modifier.fillMaxSize()) {
                    when (activePanel) {
                        AudioPlayerSheet.CHAPTERS -> ChaptersSheet(
                            tracks = tracks,
                            currentIndex = currentTrackIndex,
                            onTrackSelected = { audiobookPlayerViewModel.playTrack(it) }
                        )

                        AudioPlayerSheet.SPEED -> SpeedSheet(
                            currentSpeed = playbackSpeed,
                            onSpeedSelected = { audiobookPlayerViewModel.setPlaybackSpeed(it) }
                        )

                        AudioPlayerSheet.SLEEP -> SleepTimerSheet(
                            remainingMs = sleepTimerRemainingMs,
                            onTimerSelected = { audiobookPlayerViewModel.setSleepTimer(it) },
                            onEndOfChapterSelected = { audiobookPlayerViewModel.setSleepTimerEndOfChapter() }
                        )
                    }
                }
            }

            // BOTTOM HALF: Player Controls
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(
                        Brush.verticalGradient(
                            0f to Color.Transparent,
                            0.2f to MaterialTheme.colorScheme.background
                        )
                    )
                    .padding(horizontal = 64.dp, vertical = 24.dp)
            ) {
                // Current Chapter Pill
                if (tracks.isNotEmpty()) {
                    Box(
                        modifier = Modifier
                            .align(Alignment.CenterHorizontally)
                            .clip(RoundedCornerShape(4.dp))
                            .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.1f))
                            .border(
                                1.dp,
                                MaterialTheme.colorScheme.primary.copy(alpha = 0.2f),
                                RoundedCornerShape(4.dp)
                            )
                            .padding(horizontal = 12.dp, vertical = 4.dp)
                    ) {
                        Text(
                            text = tracks[currentTrackIndex].name.uppercase(),
                            style = MaterialTheme.typography.labelSmall.copy(
                                fontWeight = FontWeight.ExtraBold,
                                letterSpacing = 1.sp
                            ),
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                }

                Spacer(modifier = Modifier.height(24.dp))

                // Scrubber
                var isDragging by remember { mutableStateOf(false) }
                var dragPosition by remember { mutableFloatStateOf(0f) }
                val currentSliderPosition = if (isDragging) dragPosition else {
                    if (durationMs > 0) positionMs / durationMs.toFloat() else 0f
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = positionMs.toFormattedPlaybackTime(),
                        style = MaterialTheme.typography.labelSmall.copy(fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace),
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
                    )
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
                        modifier = Modifier
                            .weight(1f)
                            .padding(horizontal = 16.dp),
                        colors = SliderDefaults.colors(
                            thumbColor = MaterialTheme.colorScheme.onSurface,
                            activeTrackColor = MaterialTheme.colorScheme.primary,
                            inactiveTrackColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)
                        )
                    )
                    Text(
                        text = "- ${(durationMs - positionMs).toFormattedPlaybackTime()}",
                        style = MaterialTheme.typography.labelSmall.copy(fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace),
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
                    )
                }

                Spacer(modifier = Modifier.height(24.dp))

                // Controls Row
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Left side: Speed
                    TabButton(
                        icon = PhosphorIcons.Regular.SlidersHorizontal,
                        label = stringResource(R.string.audio_label_speed, playbackSpeed),
                        isActive = activePanel == AudioPlayerSheet.SPEED,
                        onClick = { activePanel = AudioPlayerSheet.SPEED }
                    )

                    // Center: Playback
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(32.dp)
                    ) {
                        IconButton(onClick = { audiobookPlayerViewModel.skipBackward(15) }) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Icon(
                                    PhosphorIcons.Regular.ArrowCounterClockwise,
                                    contentDescription = null,
                                    modifier = Modifier.size(32.dp)
                                )
                                Text(
                                    "-15s",
                                    style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold)
                                )
                            }
                        }

                        IconButton(
                            onClick = { audiobookPlayerViewModel.togglePlayPause() },
                            modifier = Modifier
                                .size(88.dp)
                                .clip(CircleShape)
                                .background(MaterialTheme.colorScheme.onSurface)
                        ) {
                            Icon(
                                imageVector = if (playWhenReady) PhosphorIcons.Fill.Pause else PhosphorIcons.Fill.Play,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.surface,
                                modifier = Modifier.size(40.dp)
                            )
                        }

                        IconButton(onClick = { audiobookPlayerViewModel.skipForward(15) }) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Icon(
                                    PhosphorIcons.Regular.ArrowClockwise,
                                    contentDescription = null,
                                    modifier = Modifier.size(32.dp)
                                )
                                Text(
                                    "+15s",
                                    style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold)
                                )
                            }
                        }
                    }

                    // Right side: Chapters & Timer
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        TabButton(
                            icon = PhosphorIcons.Regular.List,
                            label = stringResource(R.string.audio_label_chapters),
                            isActive = activePanel == AudioPlayerSheet.CHAPTERS,
                            onClick = { activePanel = AudioPlayerSheet.CHAPTERS }
                        )

                        IconButton(
                            onClick = { activePanel = AudioPlayerSheet.SLEEP },
                            modifier = Modifier
                                .size(48.dp)
                                .clip(CircleShape)
                                .background(
                                    if (activePanel == AudioPlayerSheet.SLEEP || sleepTimerRemainingMs != null) MaterialTheme.colorScheme.primary.copy(
                                        alpha = 0.1f
                                    ) else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.2f)
                                )
                                .border(
                                    1.dp,
                                    if (activePanel == AudioPlayerSheet.SLEEP || sleepTimerRemainingMs != null) MaterialTheme.colorScheme.primary.copy(
                                        alpha = 0.3f
                                    ) else MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.2f),
                                    CircleShape
                                )
                        ) {
                            Icon(
                                imageVector = PhosphorIcons.Regular.Clock,
                                contentDescription = null,
                                tint = if (activePanel == AudioPlayerSheet.SLEEP || sleepTimerRemainingMs != null) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun TabButton(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    isActive: Boolean,
    onClick: () -> Unit,
) {
    val backgroundColor =
        if (isActive) MaterialTheme.colorScheme.primary.copy(alpha = 0.1f) else MaterialTheme.colorScheme.surfaceVariant.copy(
            alpha = 0.1f
        )
    val borderColor =
        if (isActive) MaterialTheme.colorScheme.primary.copy(alpha = 0.3f) else MaterialTheme.colorScheme.outlineVariant.copy(
            alpha = 0.2f
        )
    val contentColor =
        if (isActive) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant

    Row(
        modifier = Modifier
            .clip(RoundedCornerShape(999.dp))
            .background(backgroundColor)
            .border(1.dp, borderColor, RoundedCornerShape(999.dp))
            .clickable { onClick() }
            .padding(horizontal = 16.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            modifier = Modifier.size(18.dp),
            tint = contentColor
        )
        Text(
            text = label,
            style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
            color = contentColor
        )
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
                        .background(if (isSelected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceContainerLowest)
                        .border(
                            1.dp,
                            if (isSelected) Color.Transparent else MaterialTheme.colorScheme.outlineVariant.copy(
                                alpha = 0.4f
                            ),
                            RoundedCornerShape(12.dp)
                        )
                        .clickable { onSpeedSelected(speed) }
                        .padding(vertical = 12.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = stringResource(R.string.audio_label_speed, speed),
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
                text = pluralStringResource(
                    R.plurals.audio_chapters_count,
                    tracks.size,
                    tracks.size
                ),
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
            stringResource(R.string.audio_sleep_timer_off) to null,
            pluralStringResource(R.plurals.audio_sleep_timer_minutes, 15, 15) to 15,
            pluralStringResource(R.plurals.audio_sleep_timer_minutes, 30, 30) to 30,
            pluralStringResource(R.plurals.audio_sleep_timer_minutes, 45, 45) to 45
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
            label = stringResource(R.string.audio_sleep_timer_end_of_chapter),
            isSelected = remainingMs == -1L,
            onClick = onEndOfChapterSelected
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
