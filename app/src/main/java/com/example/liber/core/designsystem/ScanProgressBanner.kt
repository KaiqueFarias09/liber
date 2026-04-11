package com.example.liber.core.designsystem

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.adamglin.PhosphorIcons
import com.adamglin.phosphoricons.Regular
import com.adamglin.phosphoricons.regular.FolderOpen
import com.adamglin.phosphoricons.regular.X
import com.example.liber.R
import com.example.liber.core.util.UiText
import com.example.liber.data.model.ScanState

@Composable
fun ScanProgressBanner(
    state: ScanState,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier,
) {
    AnimatedVisibility(
        visible = state !is ScanState.Idle,
        enter = expandVertically() + fadeIn(),
        exit = shrinkVertically() + fadeOut(),
        modifier = modifier,
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .background(MaterialTheme.colorScheme.primaryContainer)
                .padding(horizontal = 16.dp, vertical = 10.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.weight(1f),
                ) {
                    Icon(
                        imageVector = PhosphorIcons.Regular.FolderOpen,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onPrimaryContainer,
                        modifier = Modifier.size(18.dp),
                    )
                    Text(
                        text = bannerTitle(state).asString(),
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.onPrimaryContainer,
                    )
                }
                IconButton(
                    onClick = onDismiss,
                    enabled = state !is ScanState.Scanning,
                ) {
                    Icon(
                        imageVector = PhosphorIcons.Regular.X,
                        contentDescription = "Dismiss",
                        tint = MaterialTheme.colorScheme.onPrimaryContainer,
                        modifier = Modifier.size(18.dp),
                    )
                }
            }

            when (state) {
                is ScanState.Scanning -> {
                    val progress = if (state.total > 0) {
                        state.current.toFloat() / state.total
                    } else null
                    if (progress != null) {
                        LinearProgressIndicator(
                            progress = { progress },
                            modifier = Modifier.fillMaxWidth(),
                            color = MaterialTheme.colorScheme.onPrimaryContainer,
                            trackColor = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.24f),
                        )
                    } else {
                        LinearProgressIndicator(
                            modifier = Modifier.fillMaxWidth(),
                            color = MaterialTheme.colorScheme.onPrimaryContainer,
                            trackColor = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.24f),
                        )
                    }
                }

                else -> {}
            }
        }
    }
}

private fun bannerTitle(state: ScanState): UiText = when (state) {
    is ScanState.Scanning -> {
        val folder = state.folderName
        if (state.total <= 0) {
            UiText.StringResource(R.string.scan_state_scanning, folder)
        } else {
            UiText.StringResource(R.string.scan_state_progress, state.current, state.total, folder)
        }
    }

    is ScanState.Finished -> {
        val added = state.added
        val folder = state.folderName
        if (added == 0) UiText.StringResource(R.string.scan_state_finished_none, folder)
        else if (added == 1) UiText.StringResource(
            R.string.scan_state_finished_singular,
            added,
            folder
        )
        else UiText.StringResource(R.string.scan_state_finished_plural, added, folder)
    }

    is ScanState.Failed -> UiText.StringResource(R.string.scan_state_failed, state.reason)
    ScanState.Idle -> UiText.DynamicString("")
}
