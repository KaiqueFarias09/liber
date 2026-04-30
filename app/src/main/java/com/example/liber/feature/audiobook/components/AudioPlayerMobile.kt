package com.example.liber.feature.audiobook.components

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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.adamglin.PhosphorIcons
import com.adamglin.phosphoricons.Regular
import com.adamglin.phosphoricons.regular.ArrowLeft
import com.adamglin.phosphoricons.regular.Clock
import com.adamglin.phosphoricons.regular.DotsThree
import com.adamglin.phosphoricons.regular.List
import com.example.liber.R
import com.example.liber.core.designsystem.AudiobookCover
import com.example.liber.core.designsystem.CoverStyle
import com.example.liber.core.designsystem.responsiveMaxWidth
import com.example.liber.core.util.toFormattedPlaybackTime
import com.example.liber.data.model.Book
import com.example.liber.feature.audiobook.AudiobookPlayerViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AudioPlayerMobile(
    book: Book,
    playWhenReady: Boolean,
    positionMs: Long,
    durationMs: Long,
    currentTrackIndex: Int,
    tracks: List<AudiobookPlayerViewModel.TrackInfo>,
    isPrepared: Boolean,
    playbackSpeed: Float,
    sleepTimerRemainingMs: Long?,
    currentSliderPosition: Float,
    onPositionChange: (Float) -> Unit,
    onPositionChangeFinished: () -> Unit,
    onTogglePlayPause: () -> Unit,
    onSkipBackward: () -> Unit,
    onSkipForward: () -> Unit,
    onBack: () -> Unit,
    onShowDetails: () -> Unit,
    onOpenSpeed: () -> Unit,
    onOpenChapters: () -> Unit,
    onOpenSleepTimer: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = {
                },
                navigationIcon = {
                    AudioPlayerHeaderButton(
                        icon = PhosphorIcons.Regular.ArrowLeft,
                        onClick = onBack,
                        contentDescription = stringResource(R.string.audio_control_back),
                        modifier = Modifier.padding(start = 8.dp)
                    )
                },
                actions = {
                    AudioPlayerHeaderButton(
                        icon = PhosphorIcons.Regular.DotsThree,
                        onClick = onShowDetails,
                        contentDescription = stringResource(R.string.audio_control_more),
                        modifier = Modifier.padding(end = 8.dp)
                    )
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color.Transparent,
                    navigationIconContentColor = MaterialTheme.colorScheme.onSurface,
                    actionIconContentColor = MaterialTheme.colorScheme.onSurface
                ),
                modifier = Modifier.fillMaxWidth()
            )
        },
        containerColor = MaterialTheme.colorScheme.background,
        modifier = modifier,
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
                    AudioPlayerTrackPill(
                        text = tracks[currentTrackIndex].name,
                        modifier = Modifier.padding(bottom = 8.dp)
                    )
                }
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
                        onValueChange = onPositionChange,
                        onValueChangeFinished = onPositionChangeFinished,
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
                            text = positionMs.toFormattedPlaybackTime(),
                            style = MaterialTheme.typography.labelSmall.copy(fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace),
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            text = "- ${(durationMs - positionMs).toFormattedPlaybackTime()}",
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
                    AudioPlayerSkipButton(
                        isForward = false,
                        onClick = onSkipBackward,
                        enabled = isPrepared
                    )

                    Spacer(modifier = Modifier.size(32.dp))

                    AudioPlayerPlayPauseButton(
                        isPlaying = playWhenReady,
                        onClick = onTogglePlayPause,
                        enabled = isPrepared
                    )

                    Spacer(modifier = Modifier.size(32.dp))

                    AudioPlayerSkipButton(
                        isForward = true,
                        onClick = onSkipForward,
                        enabled = isPrepared
                    )
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
                            .clickable { onOpenSpeed() }
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
                                CircleShape,
                            )
                            .clickable { onOpenChapters() }
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

                    IconButton(onClick = onOpenSleepTimer) {
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
    }
}
