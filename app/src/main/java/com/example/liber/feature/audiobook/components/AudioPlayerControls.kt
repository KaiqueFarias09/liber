package com.example.liber.feature.audiobook.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.adamglin.PhosphorIcons
import com.adamglin.phosphoricons.Fill
import com.adamglin.phosphoricons.Regular
import com.adamglin.phosphoricons.fill.Pause
import com.adamglin.phosphoricons.fill.Play
import com.adamglin.phosphoricons.regular.ArrowClockwise
import com.adamglin.phosphoricons.regular.ArrowCounterClockwise
import com.example.liber.R

@Composable
fun AudioPlayerPlayPauseButton(
    isPlaying: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    size: Dp = 80.dp,
    iconSize: Dp = 32.dp,
    showShadow: Boolean = false
) {
    IconButton(
        onClick = onClick,
        enabled = enabled,
        modifier = modifier
            .size(size)
            .then(if (showShadow) Modifier.shadow(40.dp, shape = CircleShape) else Modifier)
            .clip(CircleShape)
            .background(
                if (isPlaying) MaterialTheme.colorScheme.surfaceContainerLowest
                else MaterialTheme.colorScheme.onSurface
            )
            .border(
                1.dp,
                if (isPlaying) MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f)
                else Color.Transparent,
                CircleShape
            )
    ) {
        Icon(
            imageVector = if (isPlaying) PhosphorIcons.Fill.Pause else PhosphorIcons.Fill.Play,
            contentDescription = if (isPlaying) stringResource(R.string.audio_control_pause)
            else stringResource(R.string.audio_control_play),
            tint = if (isPlaying) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.surface,
            modifier = Modifier.size(iconSize)
        )
    }
}

@Composable
fun AudioPlayerSkipButton(
    isForward: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    iconSize: Dp = 28.dp,
    showLabel: Boolean = true
) {
    Column(
        modifier = modifier
            .widthIn(min = 48.dp)
            .heightIn(min = 48.dp)
            .clip(CircleShape)
            .clickable(enabled = enabled) { onClick() }
            .padding(vertical = 4.dp, horizontal = 6.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        Icon(
            imageVector = if (isForward) PhosphorIcons.Regular.ArrowClockwise
            else PhosphorIcons.Regular.ArrowCounterClockwise,
            contentDescription = if (isForward) stringResource(R.string.audio_control_skip_forward)
            else stringResource(R.string.audio_control_skip_backward),
            tint = if (enabled) MaterialTheme.colorScheme.onSurface
            else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.3f),
            modifier = Modifier.size(iconSize)
        )
        if (showLabel) {
            Text(
                text = if (isForward) stringResource(R.string.audio_label_skip_forward_short)
                else stringResource(R.string.audio_label_skip_backward_short),
                style = MaterialTheme.typography.labelSmall.copy(
                    fontSize = 11.sp,
                    lineHeight = 12.sp,
                    letterSpacing = 0.sp
                ),
                fontWeight = FontWeight.Bold,
                color = if (enabled) MaterialTheme.colorScheme.onSurface.copy(alpha = 0.8f)
                else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.3f),
                textAlign = TextAlign.Center,
                maxLines = 1
            )
        }
    }
}

@Composable
fun AudioPlayerTrackPill(
    text: String,
    modifier: Modifier = Modifier,
    color: Color = MaterialTheme.colorScheme.primary
) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(4.dp))
            .background(color.copy(alpha = 0.1f))
            .border(1.dp, color.copy(alpha = 0.2f), RoundedCornerShape(4.dp))
            .padding(horizontal = 12.dp, vertical = 4.dp)
    ) {
        Text(
            text = text.uppercase(),
            style = MaterialTheme.typography.labelSmall.copy(
                fontWeight = FontWeight.ExtraBold,
                letterSpacing = 1.sp
            ),
            color = color
        )
    }
}

@Composable
fun AudioPlayerHeaderButton(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    contentDescription: String? = null,
    size: Dp = 44.dp,
    iconSize: Dp = 24.dp
) {
    Box(
        modifier = modifier
            .size(size)
            .clip(CircleShape)
            .background(MaterialTheme.colorScheme.surfaceVariant)
            .border(
                1.dp,
                MaterialTheme.colorScheme.onSurface.copy(alpha = 0.15f),
                CircleShape
            )
            .clickable(
                onClick = onClick,
                role = androidx.compose.ui.semantics.Role.Button
            ),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            imageVector = icon,
            contentDescription = contentDescription,
            tint = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.size(iconSize)
        )
    }
}
