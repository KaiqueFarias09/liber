package com.example.liber.core.designsystem

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.MenuDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.DpOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.PopupProperties

import com.example.liber.core.util.UiText

/**
 * A standardized dropdown menu with Soft Material styling.
 *
 * Corner radius: 24dp. Background uses surfaceVariant for a distinct layered look.
 * Use [LiberContextMenuItem] for items and [LiberContextMenuDivider] for separators.
 */
@Composable
fun LiberDropdownMenu(
    expanded: Boolean,
    onDismissRequest: () -> Unit,
    modifier: Modifier = Modifier,
    offset: DpOffset = DpOffset(0.dp, 0.dp),
    shape: Shape = RoundedCornerShape(24.dp),
    properties: PopupProperties = PopupProperties(focusable = true),
    content: @Composable () -> Unit,
) {
    DropdownMenu(
        expanded = expanded,
        onDismissRequest = onDismissRequest,
        modifier = modifier,
        offset = offset,
        shape = shape,
        containerColor = MaterialTheme.colorScheme.surface,
        tonalElevation = 3.dp,
        properties = properties,
    ) {
        content()
    }
}

/**
 * A single item within a [LiberDropdownMenu].
 *
 * - [label]: The text for the item.
 * - [icon]: Optional leading Phosphor icon.
 * - [destructive]: When true, renders the label and icon in the error (red) color.
 * - [onClick]: Action to perform when the item is tapped.
 */
@Composable
fun LiberContextMenuItem(
    label: UiText,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    icon: ImageVector? = null,
    destructive: Boolean = false,
    enabled: Boolean = true,
) {
    val contentColor = when {
        !enabled -> MaterialTheme.colorScheme.onSurface.copy(alpha = 0.38f)
        destructive -> MaterialTheme.colorScheme.error
        else -> MaterialTheme.colorScheme.onSurface
    }

    DropdownMenuItem(
        text = {
            Text(
                text = label.asString(),
                style = MaterialTheme.typography.bodyMedium.copy(
                    fontWeight = FontWeight.Medium,
                    fontSize = 15.sp,
                ),
                color = contentColor,
            )
        },
        onClick = onClick,
        modifier = modifier,
        enabled = enabled,
        leadingIcon = if (icon != null) {
            {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = contentColor,
                    modifier = Modifier.size(18.dp),
                )
            }
        } else null,
        colors = MenuDefaults.itemColors(
            textColor = contentColor,
            leadingIconColor = contentColor,
        ),
        contentPadding = PaddingValues(horizontal = 20.dp, vertical = 4.dp),
    )
}

/**
 * A thin horizontal divider for use inside [LiberDropdownMenu].
 */
@Composable
fun LiberContextMenuDivider() {
    HorizontalDivider(
        modifier = Modifier
            .width(240.dp),
        thickness = 1.dp,
        color = MaterialTheme.colorScheme.outline.copy(alpha = 0.25f),
    )
}
