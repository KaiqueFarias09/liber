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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
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
import com.adamglin.phosphoricons.Regular
import com.adamglin.phosphoricons.regular.ArrowLeft
import com.adamglin.phosphoricons.regular.Clock
import com.adamglin.phosphoricons.regular.DotsThree
import com.adamglin.phosphoricons.regular.List
import com.adamglin.phosphoricons.regular.SlidersHorizontal
import com.example.liber.R
import com.example.liber.core.designsystem.AudiobookCover
import com.example.liber.core.designsystem.CoverStyle
import com.example.liber.core.designsystem.Gambetta
import com.example.liber.core.designsystem.liberHorizontalDivider
import com.example.liber.core.util.toFormattedPlaybackTime
import com.example.liber.data.model.Book
import com.example.liber.feature.audiobook.AudioPlayerSheet
import com.example.liber.feature.audiobook.AudiobookPlayerViewModel

@Composable
fun AudioPlayerTabletLandscape(
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
    onTrackSelected: (Int) -> Unit,
    onSetPlaybackSpeed: (Float) -> Unit,
    onSetSleepTimer: (Int?) -> Unit,
    onSetSleepTimerEndOfChapter: () -> Unit,
    onBack: () -> Unit,
    onShowDetails: () -> Unit,
    modifier: Modifier = Modifier,
) {
    var activePanel by remember { mutableStateOf(AudioPlayerSheet.CHAPTERS) }

    Row(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        // LEFT PANE (40%)
        Column(
            modifier = Modifier
                .weight(0.4f)
                .fillMaxHeight()
                .background(MaterialTheme.colorScheme.surfaceContainerLowest),
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .statusBarsPadding()
                    .padding(horizontal = 16.dp, vertical = 16.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                AudioPlayerHeaderButton(
                    icon = PhosphorIcons.Regular.ArrowLeft,
                    onClick = onBack,
                    size = 44.dp
                )
                AudioPlayerHeaderButton(
                    icon = PhosphorIcons.Regular.DotsThree,
                    onClick = onShowDetails,
                    size = 44.dp,
                )
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
                            onTrackSelected = onTrackSelected
                        )

                        AudioPlayerSheet.SPEED -> SpeedSheet(
                            currentSpeed = playbackSpeed,
                            onSpeedSelected = onSetPlaybackSpeed
                        )

                        AudioPlayerSheet.SLEEP -> SleepTimerSheet(
                            remainingMs = sleepTimerRemainingMs,
                            onTimerSelected = onSetSleepTimer,
                            onEndOfChapterSelected = onSetSleepTimerEndOfChapter
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
                    .padding(horizontal = 64.dp, vertical = 32.dp)
            ) {
                // Current Chapter Pill
                if (tracks.isNotEmpty()) {
                    AudioPlayerTrackPill(
                        text = tracks[currentTrackIndex].name,
                        modifier = Modifier.align(Alignment.CenterHorizontally)
                    )
                }

                Spacer(modifier = Modifier.height(24.dp))

                // Scrubber
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = positionMs.toFormattedPlaybackTime(),
                        style = MaterialTheme.typography.labelSmall.copy(fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace),
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
                    )
                    Slider(
                        value = currentSliderPosition,
                        onValueChange = onPositionChange,
                        onValueChangeFinished = onPositionChangeFinished,
                        enabled = isPrepared,
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
                        AudioPlayerSkipButton(
                            isForward = false,
                            onClick = onSkipBackward,
                            enabled = isPrepared,
                            iconSize = 32.dp
                        )

                        AudioPlayerPlayPauseButton(
                            isPlaying = playWhenReady,
                            onClick = onTogglePlayPause,
                            enabled = isPrepared,
                            size = 88.dp,
                            iconSize = 40.dp
                        )

                        AudioPlayerSkipButton(
                            isForward = true,
                            onClick = onSkipForward,
                            enabled = isPrepared,
                            iconSize = 32.dp
                        )
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
                                    if ((activePanel == AudioPlayerSheet.SLEEP) || (sleepTimerRemainingMs != null)) MaterialTheme.colorScheme.primary.copy(
                                        alpha = 0.1f
                                    ) else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.2f)
                                )
                                .border(
                                    1.dp,
                                    if ((activePanel == AudioPlayerSheet.SLEEP) || (sleepTimerRemainingMs != null)) MaterialTheme.colorScheme.primary.copy(
                                        alpha = 0.3f
                                    ) else MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.2f),
                                    CircleShape
                                )
                        ) {
                            Icon(
                                imageVector = PhosphorIcons.Regular.Clock,
                                contentDescription = null,
                                tint = if ((activePanel == AudioPlayerSheet.SLEEP) || (sleepTimerRemainingMs != null)) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
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
