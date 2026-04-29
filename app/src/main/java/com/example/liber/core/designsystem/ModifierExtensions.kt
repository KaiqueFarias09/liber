package com.example.liber.core.designsystem

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * Standard maximum width for screen content to ensure readability and better UI on large screens.
 */
val MaxContentWidth = 840.dp

fun Modifier.noRippleClickable(onClick: () -> Unit): Modifier = composed {
    this.clickable(
        interactionSource = remember { MutableInteractionSource() },
        indication = null,
        onClick = onClick
    )
}

/**
 * Limits the width of a component on large screens while filling it on smaller ones.
 * Should be used in conjunction with centering.
 */
fun Modifier.responsiveMaxWidth(): Modifier = this
    .widthIn(max = MaxContentWidth)

/**
 * A reusable modifier to apply the standard Liber container style.
 *
 * It applies:
 * 1. Background color (defaults to surface)
 * 2. Border (defaults to outlineVariant with 1.dp)
 * 3. Clipping to the specified shape
 *
 * @param shape The shape of the container (defaults to 12.dp rounded corners)
 * @param backgroundColor The background color (defaults to MaterialTheme.colorScheme.surface)
 * @param borderColor The border color (defaults to MaterialTheme.colorScheme.outlineVariant)
 * @param borderWidth The thickness of the border (defaults to 1.dp)
 */
fun Modifier.liberContainer(
    shape: Shape = RoundedCornerShape(12.dp),
    backgroundColor: Color? = null,
    borderColor: Color? = null,
    borderWidth: Dp = 1.dp,
): Modifier = composed {
    val finalBgColor = backgroundColor ?: MaterialTheme.colorScheme.surface
    val finalBorderColor = borderColor ?: MaterialTheme.colorScheme.outlineVariant

    this.then(
        Modifier
            .background(finalBgColor, shape)
            .border(borderWidth, finalBorderColor, shape)
            .clip(shape)
    )
}

/**
 * A reusable modifier to apply a horizontal divider line at the bottom of a component.
 */
fun Modifier.liberHorizontalDivider(
    color: Color? = null,
    thickness: Dp = 1.dp,
): Modifier = composed {
    val finalColor = color ?: MaterialTheme.colorScheme.outlineVariant
    this.then(
        Modifier
            .background(finalColor)
            .fillMaxWidth()
            .height(thickness)
    )
}

/**
 * A reusable modifier for transparent containers that only have a border.
 *
 * @param shape The shape of the container (defaults to 12.dp rounded corners)
 * @param borderColor The border color (defaults to MaterialTheme.colorScheme.outlineVariant)
 * @param borderWidth The thickness of the border (defaults to 1.dp)
 */
fun Modifier.liberOutlinedContainer(
    shape: Shape = RoundedCornerShape(12.dp),
    borderColor: Color? = null,
    borderWidth: Dp = 1.dp,
): Modifier = composed {
    val finalBorderColor = borderColor ?: MaterialTheme.colorScheme.outlineVariant

    this.then(
        Modifier
            .border(borderWidth, finalBorderColor, shape)
            .clip(shape)
    )
}
