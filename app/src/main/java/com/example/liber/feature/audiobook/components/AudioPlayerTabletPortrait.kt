package com.example.liber.feature.audiobook.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Icon
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
import com.adamglin.phosphoricons.fill.Play
import com.adamglin.phosphoricons.regular.ArrowLeft
import com.adamglin.phosphoricons.regular.CaretDown
import com.adamglin.phosphoricons.regular.CaretUp
import com.adamglin.phosphoricons.regular.Clock
import com.adamglin.phosphoricons.regular.DotsThree
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
fun AudioPlayerTabletPortrait(
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
    var expandedTool by remember { mutableStateOf<AudioPlayerSheet?>(null) }
    val scrollState = rememberScrollState()

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background),
    ) {
        // STICKY HEADER
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(MaterialTheme.colorScheme.background)
                .statusBarsPadding()
                .padding(horizontal = 16.dp, vertical = 12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            AudioPlayerHeaderButton(
                icon = PhosphorIcons.Regular.ArrowLeft,
                onClick = onBack,
                size = 56.dp,
                iconSize = 32.dp
            )

            AudioPlayerTrackPill(
                text = stringResource(R.string.audio_label_chapters)
            )

            AudioPlayerHeaderButton(
                icon = PhosphorIcons.Regular.DotsThree,
                onClick = onShowDetails,
                size = 56.dp,
                iconSize = 32.dp
            )
        }

        // SCROLLABLE MAIN CONTENT
        Column(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .verticalScroll(scrollState)
                .padding(bottom = 64.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // COVER ART
            Box(
                modifier = Modifier
                    .padding(top = 48.dp, bottom = 40.dp)
                    .fillMaxWidth(0.55f)
                    .aspectRatio(1f)
                    .shadow(
                        60.dp,
                        shape = RoundedCornerShape(8.dp),
                        ambientColor = Color.Black,
                        spotColor = Color.Black
                    )
            ) {
                AudiobookCover(
                    book = book,
                    modifier = Modifier.fillMaxSize(),
                    style = CoverStyle.LARGE,
                    isActive = true,
                    isPlaying = playWhenReady
                )
            }

            // SCRUBBER
            Column(
                modifier = Modifier
                    .fillMaxWidth(0.7f)
                    .padding(bottom = 48.dp)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 12.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.Bottom
                ) {
                    Text(
                        text = positionMs.toFormattedPlaybackTime(),
                        style = MaterialTheme.typography.labelMedium.copy(fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace),
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
                    )
                    Text(
                        text = "- ${(durationMs - positionMs).toFormattedPlaybackTime()}",
                        style = MaterialTheme.typography.labelMedium.copy(fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace),
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
                    )
                }

                Slider(
                    value = currentSliderPosition,
                    onValueChange = onPositionChange,
                    onValueChangeFinished = onPositionChangeFinished,
                    enabled = isPrepared,
                    modifier = Modifier.fillMaxWidth(),
                    colors = SliderDefaults.colors(
                        thumbColor = MaterialTheme.colorScheme.onSurface,
                        activeTrackColor = MaterialTheme.colorScheme.primary,
                        inactiveTrackColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)
                    )
                )
            }

            // MAIN CONTROLS
            Row(
                modifier = Modifier
                    .fillMaxWidth(0.7f)
                    .padding(bottom = 56.dp),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                AudioPlayerSkipButton(
                    isForward = false,
                    onClick = onSkipBackward,
                    enabled = isPrepared,
                    iconSize = 36.dp
                )

                Spacer(modifier = Modifier.width(64.dp))

                AudioPlayerPlayPauseButton(
                    isPlaying = playWhenReady,
                    onClick = onTogglePlayPause,
                    enabled = isPrepared,
                    size = 112.dp,
                    iconSize = 48.dp,
                    showShadow = true
                )

                Spacer(modifier = Modifier.width(64.dp))

                AudioPlayerSkipButton(
                    isForward = true,
                    onClick = onSkipForward,
                    enabled = isPrepared,
                    iconSize = 36.dp
                )
            }

            // INLINE TOOLS
            Column(
                modifier = Modifier
                    .fillMaxWidth(0.7f)
                    .padding(bottom = 64.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    InlineToolButton(
                        icon = PhosphorIcons.Regular.SlidersHorizontal,
                        label = stringResource(R.string.audio_playback_speed),
                        value = "${playbackSpeed}x",
                        isExpanded = expandedTool == AudioPlayerSheet.SPEED,
                        onClick = {
                            expandedTool =
                                if (expandedTool == AudioPlayerSheet.SPEED) null else AudioPlayerSheet.SPEED
                        },
                        modifier = Modifier.weight(1f)
                    )
                    InlineToolButton(
                        icon = PhosphorIcons.Regular.Clock,
                        label = stringResource(R.string.audio_sleep_timer),
                        value = if (sleepTimerRemainingMs != null) {
                            if (sleepTimerRemainingMs == -1L) "End" else "${(sleepTimerRemainingMs / 60000).toInt()}m"
                        } else "Off",
                        isExpanded = expandedTool == AudioPlayerSheet.SLEEP,
                        isTimerActive = sleepTimerRemainingMs != null,
                        onClick = {
                            expandedTool =
                                if (expandedTool == AudioPlayerSheet.SLEEP) null else AudioPlayerSheet.SLEEP
                        },
                        modifier = Modifier.weight(1f)
                    )
                }

                // Expanded Speed
                AnimatedVisibility(
                    visible = expandedTool == AudioPlayerSheet.SPEED,
                    enter = expandVertically() + fadeIn(),
                    exit = shrinkVertically() + fadeOut()
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(24.dp))
                            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.1f))
                            .border(
                                1.dp,
                                MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.1f),
                                RoundedCornerShape(24.dp)
                            )
                            .padding(24.dp)
                    ) {
                        SpeedSheet(
                            currentSpeed = playbackSpeed,
                        ) {
                            onSetPlaybackSpeed(it)
                            expandedTool = null
                        }
                    }
                }

                // Expanded Timer
                AnimatedVisibility(
                    visible = expandedTool == AudioPlayerSheet.SLEEP,
                    enter = expandVertically() + fadeIn(),
                    exit = shrinkVertically() + fadeOut()
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(24.dp))
                            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.1f))
                            .border(
                                1.dp,
                                MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.1f),
                                RoundedCornerShape(24.dp)
                            )
                            .padding(16.dp)
                    ) {
                        SleepTimerSheet(
                            remainingMs = sleepTimerRemainingMs,
                            onTimerSelected = {
                                onSetSleepTimer(it)
                                expandedTool = null
                            },
                            onEndOfChapterSelected = {
                                onSetSleepTimerEndOfChapter()
                                expandedTool = null
                            }
                        )
                    }
                }
            }

            // CHAPTERS LIST
            Column(
                modifier = Modifier.fillMaxWidth(0.78f)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 24.dp)
                        .liberHorizontalDivider(
                            color = MaterialTheme.colorScheme.outlineVariant.copy(
                                alpha = 0.1f
                            )
                        ),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.Bottom
                ) {
                    Text(
                        text = stringResource(R.string.audio_chapters),
                        style = MaterialTheme.typography.headlineMedium.copy(fontFamily = Gambetta),
                        modifier = Modifier.padding(bottom = 16.dp)
                    )
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
                        modifier = Modifier.padding(bottom = 16.dp)
                    )
                }

                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    tracks.forEachIndexed { index, track ->
                        val isActive = index == currentTrackIndex
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(16.dp))
                                .background(
                                    if (isActive) MaterialTheme.colorScheme.primary.copy(
                                        alpha = 0.1f
                                    ) else Color.Transparent
                                )
                                .border(
                                    1.dp,
                                    if (isActive) MaterialTheme.colorScheme.primary.copy(alpha = 0.2f) else Color.Transparent,
                                    RoundedCornerShape(16.dp)
                                )
                                .clickable { onTrackSelected(index) }
                                .padding(horizontal = 24.dp, vertical = 20.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(16.dp)
                        ) {
                            if (isActive) {
                                Box(
                                    modifier = Modifier
                                        .size(24.dp)
                                        .clip(CircleShape)
                                        .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.2f)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = PhosphorIcons.Fill.Play,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.size(12.dp)
                                    )
                                }
                            } else {
                                Text(
                                    text = (index + 1).toString(),
                                    style = MaterialTheme.typography.labelSmall.copy(fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace),
                                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.3f),
                                    modifier = Modifier.width(24.dp),
                                    textAlign = TextAlign.Center
                                )
                            }

                            Text(
                                text = track.name,
                                style = MaterialTheme.typography.bodyLarge,
                                fontWeight = if (isActive) FontWeight.SemiBold else FontWeight.Medium,
                                color = if (isActive) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface,
                                modifier = Modifier.weight(1f),
                                maxLines = 1
                            )

                            Text(
                                text = track.durationMs.toFormattedPlaybackTime(),
                                style = MaterialTheme.typography.labelMedium.copy(fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace),
                                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f)
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun InlineToolButton(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    value: String,
    isExpanded: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    isTimerActive: Boolean = false,
) {
    val isActive = isExpanded || isTimerActive
    val backgroundColor =
        if (isActive) MaterialTheme.colorScheme.primary.copy(alpha = 0.1f) else MaterialTheme.colorScheme.surfaceVariant.copy(
            alpha = 0.05f
        )
    val borderColor =
        if (isActive) MaterialTheme.colorScheme.primary.copy(alpha = 0.3f) else MaterialTheme.colorScheme.outlineVariant.copy(
            alpha = 0.05f
        )
    val contentColor =
        if (isActive) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant

    Row(
        modifier = modifier
            .clip(RoundedCornerShape(16.dp))
            .background(backgroundColor)
            .border(1.dp, borderColor, RoundedCornerShape(16.dp))
            .clickable { onClick() }
            .padding(horizontal = 24.dp, vertical = 16.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                modifier = Modifier.size(20.dp),
                tint = contentColor
            )
            Text(
                text = label.uppercase(),
                style = MaterialTheme.typography.labelSmall.copy(
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 1.sp
                ),
                color = contentColor
            )
        }
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(
                text = value,
                style = MaterialTheme.typography.labelMedium.copy(fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace),
                color = contentColor
            )
            Icon(
                imageVector = if (isExpanded) PhosphorIcons.Regular.CaretUp else PhosphorIcons.Regular.CaretDown,
                contentDescription = null,
                modifier = Modifier.size(16.dp),
                tint = contentColor.copy(alpha = 0.5f)
            )
        }
    }
}
