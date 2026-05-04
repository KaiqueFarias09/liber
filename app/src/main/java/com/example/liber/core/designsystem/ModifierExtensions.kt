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
import androidx.compose.material3.surfaceColorAtElevation
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.compositeOver
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
 * 1. Background color (defaults to surface with tonal elevation)
 * 2. Border (defaults to outlineVariant with 1.dp)
 * 3. Clipping to the specified shape
 *
 * @param shape The shape of the container (defaults to 12.dp rounded corners)
 * @param backgroundColor The background color (defaults to MaterialTheme.colorScheme.surface with elevation tint)
 * @param borderColor The border color (defaults to MaterialTheme.colorScheme.outlineVariant)
 * @param borderWidth The thickness of the border (defaults to 1.dp)
 * @param elevation The tonal elevation to apply to the background color (defaults to 0.dp)
 */
fun Modifier.liberContainer(
    shape: Shape = RoundedCornerShape(12.dp),
    backgroundColor: Color? = null,
    borderColor: Color? = null,
    borderWidth: Dp = 1.dp,
    elevation: Dp = 0.dp,
): Modifier = composed {
    val finalBgColor =
        backgroundColor ?: MaterialTheme.colorScheme.surfaceColorAtElevation(elevation)
    val finalBorderColor = borderColor ?: MaterialTheme.colorScheme.outlineVariant

    this.then(
        Modifier
            .background(finalBgColor, shape)
            .border(borderWidth, finalBorderColor, shape)
            .clip(shape)
    )
}

/**
 * A container with a background and border tinted with the app's accent (primary) color.
 *
 * @param shape The shape of the container.
 * @param borderWidth The thickness of the border.
 * @param tintAlpha The alpha of the accent color to composite over the base color.
 * @param baseColor The base background color (defaults to surface).
 */
fun Modifier.liberAccentContainer(
    shape: Shape = RoundedCornerShape(12.dp),
    borderWidth: Dp = 1.dp,
    tintAlpha: Float = 0.04f,
    baseColor: Color? = null,
): Modifier = composed {
    val accentColor = MaterialTheme.colorScheme.primary
    val resolvedBaseColor = baseColor ?: MaterialTheme.colorScheme.surface
    val containerColor = accentColor.copy(alpha = tintAlpha).compositeOver(resolvedBaseColor)
    val borderColor = accentColor.copy(alpha = tintAlpha * 3f)
        .compositeOver(MaterialTheme.colorScheme.outlineVariant)

    this.then(
        Modifier
            .background(containerColor, shape)
            .border(borderWidth, borderColor, shape)
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
