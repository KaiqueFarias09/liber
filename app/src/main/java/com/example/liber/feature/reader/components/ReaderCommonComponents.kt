package com.example.liber.feature.reader.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.liber.R
import com.example.liber.core.util.UiText
import com.example.liber.feature.reader.SelectionAnchor
import org.coolreader.crengine.TOCItem

@Composable
fun TocItemRow(item: TOCItem, onClick: () -> Unit) {
    val depth = (item.mLevel - 1).coerceAtLeast(0)
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(
                start = (depth * 20 + 4).dp,
                end = 4.dp,
                top = 12.dp,
                bottom = 12.dp,
            ),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = item.mName ?: stringResource(R.string.reader_label_untitled),
            style = MaterialTheme.typography.bodyLarge,
            fontWeight = if (depth == 0) FontWeight.SemiBold else FontWeight.Normal,
            color = if (depth == 0) MaterialTheme.colorScheme.onSurface
            else MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.weight(1f),
        )
    }
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(0.5.dp)
            .background(MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
    )
}

@Composable
fun ProgressScrubber(
    progress: Float,
    onProgressChange: (Float) -> Unit,
    trackColor: Color,
    progressColor: Color,
    modifier: Modifier = Modifier,
) {
    var draggingProgress by remember { mutableStateOf<Float?>(null) }
    val displayProgress = draggingProgress ?: progress

    Canvas(
        modifier = modifier
            .height(32.dp)
            .pointerInput(Unit) {
                detectTapGestures { offset ->
                    onProgressChange((offset.x / size.width).coerceIn(0f, 1f))
                }
            }
            .pointerInput(Unit) {
                detectDragGestures(
                    onDragStart = { offset ->
                        draggingProgress = (offset.x / size.width).coerceIn(0f, 1f)
                    },
                    onDragEnd = {
                        draggingProgress?.let { onProgressChange(it) }
                        draggingProgress = null
                    },
                    onDragCancel = { draggingProgress = null },
                    onDrag = { change, _ ->
                        draggingProgress = (change.position.x / size.width).coerceIn(0f, 1f)
                    }
                )
            }
    ) {
        val trackH = 4.dp.toPx()
        val trackY = (size.height - trackH) / 2f
        val clamped = displayProgress.coerceIn(0f, 1f)

        drawRoundRect(
            color = trackColor, topLeft = Offset(0f, trackY),
            size = Size(size.width, trackH), cornerRadius = CornerRadius(trackH / 2f),
        )
        val fillW = size.width * clamped
        if (fillW > 0f) {
            drawRoundRect(
                color = progressColor, topLeft = Offset(0f, trackY),
                size = Size(fillW, trackH), cornerRadius = CornerRadius(trackH / 2f),
            )
        }
        val thumbR = 8.dp.toPx()
        val thumbX = fillW.coerceIn(thumbR, size.width - thumbR)

        drawCircle(
            color = progressColor,
            radius = thumbR,
            center = Offset(thumbX, size.height / 2f)
        )
    }
}

@Composable
fun SelectionHandle(
    anchor: SelectionAnchor,
    isStart: Boolean,
    onDrag: (x: Int, y: Int) -> Unit,
    onDragEnd: () -> Unit = {},
) {
    val density = LocalDensity.current
    val handleSizePx = with(density) { 20.dp.roundToPx() }
    val color = MaterialTheme.colorScheme.primary

    var cumDx by remember { mutableStateOf(0f) }
    var cumDy by remember { mutableStateOf(0f) }
    LaunchedEffect(anchor) {
        cumDx = 0f
        cumDy = 0f
    }

    Box(
        modifier = Modifier
            .offset {
                val baseX = if (isStart) anchor.x.toInt() - handleSizePx else anchor.x.toInt()
                IntOffset(baseX + cumDx.toInt(), anchor.y.toInt() + cumDy.toInt())
            }
            .size(40.dp)
            .pointerInput(anchor) {
                detectDragGestures(
                    onDrag = { _, dragAmount ->
                        cumDx += dragAmount.x
                        cumDy += dragAmount.y
                        val queryX =
                            anchor.x + (if (isStart) -handleSizePx / 2f else handleSizePx / 2f) + cumDx
                        val queryY = anchor.y + cumDy
                        onDrag(queryX.toInt(), queryY.toInt())
                    },
                    onDragEnd = { onDragEnd() },
                    onDragCancel = { onDragEnd() },
                )
            },
        contentAlignment = Alignment.Center,
    ) {
        Box(
            modifier = Modifier
                .size(20.dp)
                .background(color, CircleShape)
        )
    }
}

@Composable
fun ReaderNavItem(
    icon: @Composable () -> Unit,
    label: UiText,
    labelColor: Color,
    onClick: () -> Unit,
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .clickable(onClick = onClick)
            .padding(horizontal = 12.dp, vertical = 8.dp),
    ) {
        icon()
        Spacer(Modifier.height(4.dp))
        Text(
            text = label.asString(),
            style = MaterialTheme.typography.labelSmall,
            color = labelColor,
            fontSize = 10.sp,
        )
    }
}
