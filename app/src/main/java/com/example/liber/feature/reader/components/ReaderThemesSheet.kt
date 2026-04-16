package com.example.liber.feature.reader.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.adamglin.PhosphorIcons
import com.adamglin.phosphoricons.Regular
import com.adamglin.phosphoricons.regular.FrameCorners
import com.adamglin.phosphoricons.regular.LineSegments
import com.adamglin.phosphoricons.regular.Minus
import com.adamglin.phosphoricons.regular.Paragraph
import com.adamglin.phosphoricons.regular.TextAa
import com.example.liber.R
import com.example.liber.feature.reader.ReaderTheme
import com.example.liber.feature.reader.ReaderThemes

@Composable
fun ThemesSheet(
    currentThemeId: String,
    onThemeChange: (String) -> Unit,
    fontSize: Double,
    onDecreaseFontSize: () -> Unit,
    onIncreaseFontSize: () -> Unit,
    pageScroll: Boolean,
    onPageScrollChange: (Boolean) -> Unit,
    customizeLayout: Boolean,
    onCustomizeLayoutChange: (Boolean) -> Unit,
    lineSpacing: Double,
    onLineSpacingChange: (Double) -> Unit,
    characterSpacing: Double,
    onCharacterSpacingChange: (Double) -> Unit,
    wordSpacing: Double,
    onWordSpacingChange: (Double) -> Unit,
    margins: Double,
    onMarginsChange: (Double) -> Unit,
    onResetSettings: () -> Unit,
) {
    val scrollState = rememberScrollState()
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .verticalScroll(scrollState)
            .padding(horizontal = 20.dp),
    ) {
        Spacer(Modifier.height(16.dp))

        // ── Font size row ─────────────────────────────────────────────────────
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    MaterialTheme.colorScheme.surfaceContainerLow,
                    RoundedCornerShape(12.dp)
                )
                .border(
                    0.5.dp,
                    MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f),
                    RoundedCornerShape(12.dp)
                ),
        ) {
            Box(
                modifier = Modifier
                    .weight(1f)
                    .clickable(onClick = onDecreaseFontSize)
                    .padding(vertical = 14.dp),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    PhosphorIcons.Regular.Minus,
                    contentDescription = stringResource(R.string.reader_themes_decrease_font),
                    tint = MaterialTheme.colorScheme.onSurface, modifier = Modifier.size(20.dp)
                )
            }
            Box(
                modifier = Modifier
                    .width(0.5.dp)
                    .height(48.dp)
                    .background(MaterialTheme.colorScheme.outlineVariant)
                    .align(Alignment.CenterVertically)
            )
            Box(
                modifier = Modifier
                    .weight(2f)
                    .padding(vertical = 14.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    "${(16.0 * fontSize).toInt()} sp",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurface
                )
            }
            Box(
                modifier = Modifier
                    .width(0.5.dp)
                    .height(48.dp)
                    .background(MaterialTheme.colorScheme.outlineVariant)
                    .align(Alignment.CenterVertically)
            )
            Box(
                modifier = Modifier
                    .weight(1f)
                    .clickable(onClick = onIncreaseFontSize)
                    .padding(vertical = 14.dp),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    PhosphorIcons.Regular.TextAa,
                    contentDescription = stringResource(R.string.reader_themes_increase_font),
                    tint = MaterialTheme.colorScheme.onSurface, modifier = Modifier.size(24.dp)
                )
            }
        }

        Spacer(Modifier.height(16.dp))

        // ── Theme grid (3 columns × 2 rows) ───────────────────────────────────
        ReaderThemes.chunked(3).forEach { rowThemes ->
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                rowThemes.forEach { t ->
                    ThemePreviewTile(
                        theme = t, isSelected = currentThemeId == t.id,
                        onClick = { onThemeChange(t.id) }, modifier = Modifier.weight(1f),
                    )
                }
                repeat(3 - rowThemes.size) { Spacer(Modifier.weight(1f)) }
            }
            Spacer(Modifier.height(12.dp))
        }

        // ── Accessibility & Layout Options ────────────────────────────────────
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(0.5.dp)
                .background(MaterialTheme.colorScheme.outlineVariant)
        )
        Spacer(Modifier.height(16.dp))

        Text(
            stringResource(R.string.reader_themes_accessibility_layout),
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.onSurface
        )
        Spacer(Modifier.height(12.dp))

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    MaterialTheme.colorScheme.surfaceContainerLow,
                    RoundedCornerShape(12.dp)
                )
                .border(
                    0.5.dp,
                    MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f),
                    RoundedCornerShape(12.dp)
                ),
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 4.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    stringResource(R.string.reader_themes_scroll_mode),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Switch(checked = pageScroll, onCheckedChange = onPageScrollChange)
            }
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(0.5.dp)
                    .background(MaterialTheme.colorScheme.outlineVariant)
            )
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 4.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    stringResource(R.string.reader_themes_customize),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Switch(checked = customizeLayout, onCheckedChange = onCustomizeLayoutChange)
            }
        }

        AnimatedVisibility(
            visible = customizeLayout,
            enter = expandVertically(),
            exit = shrinkVertically()
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 8.dp)
                    .background(
                        MaterialTheme.colorScheme.surfaceContainerLow,
                        RoundedCornerShape(12.dp)
                    )
                    .border(
                        0.5.dp,
                        MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f),
                        RoundedCornerShape(12.dp)
                    )
                    .padding(horizontal = 16.dp, vertical = 12.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                LayoutSliderRow(
                    stringResource(R.string.reader_themes_line_spacing),
                    icon = {
                        Icon(
                            PhosphorIcons.Regular.LineSegments,
                            null,
                            Modifier.size(18.dp),
                            MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    },
                    value = lineSpacing.toFloat(), valueText = "${"%.2f".format(lineSpacing)}",
                    valueRange = 0.8f..2.5f, onValueChange = { onLineSpacingChange(it.toDouble()) })
                Divider()
                LayoutSliderRow(
                    stringResource(R.string.reader_themes_character_spacing),
                    icon = {
                        Text(
                            "Abc",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    },
                    value = characterSpacing.toFloat().coerceIn(0f, 20f),
                    valueText = "${characterSpacing.toInt().coerceIn(0, 20)}",
                    valueRange = 0f..20f,
                    onValueChange = { onCharacterSpacingChange(it.toDouble()) })
                Divider()
                LayoutSliderRow(
                    stringResource(R.string.reader_themes_word_spacing),
                    icon = {
                        Icon(
                            PhosphorIcons.Regular.Paragraph,
                            null,
                            Modifier.size(18.dp),
                            MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    },
                    value = wordSpacing.toFloat(),
                    valueText = "${100 + wordSpacing.toInt()}%",
                    valueRange = -20f..20f, onValueChange = { onWordSpacingChange(it.toDouble()) })
                Divider()
                LayoutSliderRow(
                    stringResource(R.string.reader_themes_margins),
                    icon = {
                        Icon(
                            PhosphorIcons.Regular.FrameCorners,
                            null,
                            Modifier.size(18.dp),
                            MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    },
                    value = margins.toFloat(),
                    valueText = "${margins.toInt()} dp",
                    valueRange = 0f..60f, onValueChange = { onMarginsChange(it.toDouble()) })
            }
        }

        Spacer(Modifier.height(16.dp))
        Button(
            onClick = onResetSettings, modifier = Modifier.fillMaxWidth(),
            colors = ButtonDefaults.buttonColors(
                containerColor = MaterialTheme.colorScheme.surfaceContainerLow,
                contentColor = MaterialTheme.colorScheme.onSurface
            ),
            shape = RoundedCornerShape(12.dp),
            border = BorderStroke(
                0.5.dp,
                MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)
            )
        ) {
            Text(
                stringResource(R.string.reader_themes_reset),
                style = MaterialTheme.typography.bodyMedium,
                modifier = Modifier.padding(vertical = 4.dp)
            )
        }
        Spacer(Modifier.height(24.dp))
    }
}

@Composable
private fun ThemePreviewTile(
    theme: ReaderTheme,
    isSelected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier = modifier
            .height(96.dp)
            .background(theme.background, RoundedCornerShape(16.dp))
            .then(
                if (isSelected)
                    Modifier.border(3.dp, Color(0xFF007AFF), RoundedCornerShape(16.dp))
                else
                    Modifier.border(
                        1.5.dp,
                        Color.Gray.copy(alpha = 0.3f),
                        RoundedCornerShape(16.dp)
                    )
            )
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                "Aa",
                fontSize = 26.sp,
                fontWeight = if (theme.id == "bold") FontWeight.Bold else FontWeight.Normal,
                color = theme.textColor
            )
            Spacer(Modifier.height(4.dp))
            Text(
                theme.name.asString(),
                style = MaterialTheme.typography.labelSmall,
                color = theme.textColor.copy(alpha = 0.6f)
            )
        }
    }
}

@Composable
private fun LayoutSliderRow(
    label: String,
    icon: @Composable () -> Unit,
    value: Float,
    valueText: String,
    valueRange: ClosedFloatingPointRange<Float>,
    onValueChange: (Float) -> Unit,
) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                label, style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant, letterSpacing = 0.5.sp
            )
            Text(
                valueText,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        Spacer(Modifier.height(2.dp))
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            icon()
            Slider(
                value = value,
                onValueChange = onValueChange,
                valueRange = valueRange,
                modifier = Modifier.weight(1f)
            )
        }
    }
}

@Composable
private fun Divider() {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(0.5.dp)
            .background(MaterialTheme.colorScheme.outlineVariant)
    )
}
