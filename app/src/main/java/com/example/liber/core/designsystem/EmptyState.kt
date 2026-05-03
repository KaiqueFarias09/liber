package com.example.liber.core.designsystem

import androidx.annotation.DrawableRes
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.liber.core.util.UiText

@Composable
fun EmptyState(
    title: UiText,
    modifier: Modifier = Modifier,
    subtitle: UiText? = null,
    @DrawableRes image: Int? = null,
    icon: ImageVector? = null,
    actionLabel: UiText? = null,
    onAction: (() -> Unit)? = null,
    showImage: Boolean = true,
) {
    Column(
        modifier = modifier
            .fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        // --- SKETCHBOOK / POLAROID CONTAINER ---
        Surface(
            modifier = Modifier
                .widthIn(max = 360.dp)
                .fillMaxWidth()
                .aspectRatio(1f),
            color = MaterialTheme.colorScheme.surface,
            border = BorderStroke(
                1.dp,
                MaterialTheme.colorScheme.outlineVariant
            ),
            shape = RoundedCornerShape(2.dp),
            tonalElevation = 0.dp
        ) {
            val cornerColor = MaterialTheme.colorScheme.outlineVariant
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .drawBehind {
                        val cornerSize = 8.dp.toPx()
                        val strokeWidth = 1.dp.toPx()
                        val margin = 8.dp.toPx()
                        val color = cornerColor

                        // Top Left
                        drawRect(color, Offset(margin, margin), Size(cornerSize, strokeWidth))
                        drawRect(color, Offset(margin, margin), Size(strokeWidth, cornerSize))

                        // Top Right
                        drawRect(
                            color,
                            Offset(size.width - cornerSize - margin, margin),
                            Size(cornerSize, strokeWidth)
                        )
                        drawRect(
                            color,
                            Offset(size.width - strokeWidth - margin, margin),
                            Size(strokeWidth, cornerSize)
                        )

                        // Bottom Left
                        drawRect(
                            color,
                            Offset(margin, size.height - strokeWidth - margin),
                            Size(cornerSize, strokeWidth)
                        )
                        drawRect(
                            color,
                            Offset(margin, size.height - cornerSize - margin),
                            Size(strokeWidth, cornerSize)
                        )

                        // Bottom Right
                        drawRect(
                            color,
                            Offset(
                                size.width - cornerSize - margin,
                                size.height - strokeWidth - margin
                            ),
                            Size(cornerSize, strokeWidth)
                        )
                        drawRect(
                            color,
                            Offset(
                                size.width - strokeWidth - margin,
                                size.height - cornerSize - margin
                            ),
                            Size(strokeWidth, cornerSize)
                        )
                    },
                contentAlignment = Alignment.Center
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center,
                    modifier = Modifier.fillMaxSize()
                ) {
                    // Part 1: Illustration (weighted to take most space)
                    Box(
                        modifier = Modifier
                            .weight(1.3f)
                            .fillMaxWidth(),
                        contentAlignment = Alignment.Center
                    ) {
                        if (showImage && image != null) {
                            Image(
                                painter = painterResource(image),
                                contentDescription = null,
                                modifier = Modifier
                                    .fillMaxSize()
                                    .padding(20.dp), // Sufficient breathing room from edges
                                colorFilter = if (MaterialTheme.extendedColors.isDark) ColorFilter.tint(
                                    color = MaterialTheme.colorScheme.onSurface,
                                    blendMode = BlendMode.SrcIn
                                ) else null
                            )
                        } else if (icon != null) {
                            Icon(
                                imageVector = icon,
                                contentDescription = null,
                                modifier = Modifier.size(84.dp),
                                tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.8f)
                            )
                        }
                    }

                    // Part 2: Text & Action
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 24.dp)
                            .padding(bottom = 24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        Text(
                            text = title.asString(),
                            style = MaterialTheme.typography.headlineSmall.copy(
                                fontFamily = Gambetta,
                                fontSize = 20.sp
                            ),
                            color = MaterialTheme.colorScheme.onSurface,
                            textAlign = TextAlign.Center,
                        )

                        if (subtitle != null) {
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = subtitle.asString(),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                textAlign = TextAlign.Center,
                                lineHeight = 18.sp,
                            )
                        }

                        if (actionLabel != null && onAction != null) {
                            Spacer(Modifier.height(16.dp))
                            LiberButton(
                                text = actionLabel,
                                onClick = onAction,
                                type = LiberButtonType.PRIMARY,
                                modifier = Modifier.height(36.dp)
                            )
                        }
                    }
                }
            }
        }
    }
}
