package com.example.liber.feature.audiobook

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.example.liber.R
import com.example.liber.core.designsystem.LiberDialog
import com.example.liber.core.designsystem.LiberModalBottomSheet
import com.example.liber.core.util.UiText
import com.example.liber.data.model.Book
import com.example.liber.feature.audiobook.components.AudioPlayerMobile
import com.example.liber.feature.audiobook.components.AudioPlayerTabletLandscape
import com.example.liber.feature.audiobook.components.AudioPlayerTabletPortrait
import com.example.liber.feature.audiobook.components.ChaptersSheet
import com.example.liber.feature.audiobook.components.SleepTimerSheet
import com.example.liber.feature.audiobook.components.SpeedSheet
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
        val isTabletPortrait = maxWidth >= 600.dp && maxWidth <= maxHeight

        BackHandler {
            if (!(isTabletLandscape || isTabletPortrait) && activeSheet != null) {
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
                isPrepared = isPrepared,
                playbackSpeed = playbackSpeed,
                sleepTimerRemainingMs = sleepTimerRemainingMs,
                currentSliderPosition = currentSliderPosition,
                onPositionChange = {
                    isDragging = true
                    dragPosition = it
                },
                onPositionChangeFinished = {
                    audiobookPlayerViewModel.seekTo((dragPosition * durationMs).toLong())
                    isDragging = false
                },
                onTogglePlayPause = { audiobookPlayerViewModel.togglePlayPause() },
                onSkipBackward = { audiobookPlayerViewModel.skipBackward(15) },
                onSkipForward = { audiobookPlayerViewModel.skipForward(15) },
                onTrackSelected = { audiobookPlayerViewModel.playTrack(it) },
                onSetPlaybackSpeed = { audiobookPlayerViewModel.setPlaybackSpeed(it) },
                onSetSleepTimer = { audiobookPlayerViewModel.setSleepTimer(it) },
                onSetSleepTimerEndOfChapter = { audiobookPlayerViewModel.setSleepTimerEndOfChapter() },
                onBack = onBack,
                onShowDetails = { showDetailsSheet = true }
            )
        } else if (isTabletPortrait) {
            AudioPlayerTabletPortrait(
                book = book,
                playWhenReady = playWhenReady,
                positionMs = positionMs,
                durationMs = durationMs,
                currentTrackIndex = currentTrackIndex,
                tracks = tracks,
                isPrepared = isPrepared,
                playbackSpeed = playbackSpeed,
                sleepTimerRemainingMs = sleepTimerRemainingMs,
                currentSliderPosition = currentSliderPosition,
                onPositionChange = {
                    isDragging = true
                    dragPosition = it
                },
                onPositionChangeFinished = {
                    audiobookPlayerViewModel.seekTo((dragPosition * durationMs).toLong())
                    isDragging = false
                },
                onTogglePlayPause = { audiobookPlayerViewModel.togglePlayPause() },
                onSkipBackward = { audiobookPlayerViewModel.skipBackward(15) },
                onSkipForward = { audiobookPlayerViewModel.skipForward(15) },
                onTrackSelected = { audiobookPlayerViewModel.playTrack(it) },
                onSetPlaybackSpeed = { audiobookPlayerViewModel.setPlaybackSpeed(it) },
                onSetSleepTimer = { audiobookPlayerViewModel.setSleepTimer(it) },
                onSetSleepTimerEndOfChapter = { audiobookPlayerViewModel.setSleepTimerEndOfChapter() },
                onBack = onBack,
                onShowDetails = { showDetailsSheet = true }
            )
        } else {
            AudioPlayerMobile(
                book = book,
                playWhenReady = playWhenReady,
                positionMs = positionMs,
                durationMs = durationMs,
                currentTrackIndex = currentTrackIndex,
                tracks = tracks,
                isPrepared = isPrepared,
                playbackSpeed = playbackSpeed,
                sleepTimerRemainingMs = sleepTimerRemainingMs,
                currentSliderPosition = currentSliderPosition,
                onPositionChange = {
                    isDragging = true
                    dragPosition = it
                },
                onPositionChangeFinished = {
                    audiobookPlayerViewModel.seekTo((dragPosition * durationMs).toLong())
                    isDragging = false
                },
                onTogglePlayPause = { audiobookPlayerViewModel.togglePlayPause() },
                onSkipBackward = { audiobookPlayerViewModel.skipBackward(15) },
                onSkipForward = { audiobookPlayerViewModel.skipForward(15) },
                onBack = onBack,
                onShowDetails = { showDetailsSheet = true },
                onOpenSpeed = { activeSheet = AudioPlayerSheet.SPEED },
                onOpenChapters = { activeSheet = AudioPlayerSheet.CHAPTERS },
                onOpenSleepTimer = { activeSheet = AudioPlayerSheet.SLEEP }
            )
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

enum class AudioPlayerSheet {
    SPEED, CHAPTERS, SLEEP
}
