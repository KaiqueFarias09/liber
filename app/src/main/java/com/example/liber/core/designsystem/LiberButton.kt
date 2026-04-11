package com.example.liber.core.designsystem

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

enum class LiberButtonType {
    PRIMARY,
    PRIMARY_INVERTED,
    SECONDARY,
    TERTIARY
}

@Composable
fun LiberButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    type: LiberButtonType = LiberButtonType.PRIMARY,
    enabled: Boolean = true,
) {
    val isDark = MaterialTheme.extendedColors.isDark

    val containerColor = when (type) {
        LiberButtonType.PRIMARY -> MaterialTheme.colorScheme.primary
        LiberButtonType.PRIMARY_INVERTED -> if (isDark) Color.White else MaterialTheme.colorScheme.onPrimary
        LiberButtonType.SECONDARY -> MaterialTheme.colorScheme.secondary
        LiberButtonType.TERTIARY -> MaterialTheme.colorScheme.tertiary
    }

    val contentColor = when (type) {
        LiberButtonType.PRIMARY -> MaterialTheme.colorScheme.onPrimary
        LiberButtonType.PRIMARY_INVERTED -> if (isDark) MaterialTheme.colorScheme.background else MaterialTheme.colorScheme.primary
        LiberButtonType.SECONDARY -> MaterialTheme.colorScheme.onSecondary
        LiberButtonType.TERTIARY -> MaterialTheme.colorScheme.onTertiary
    }

    Button(
        onClick = onClick,
        modifier = modifier.height(48.dp),
        enabled = enabled,
        colors = ButtonDefaults.buttonColors(
            containerColor = containerColor,
            contentColor = contentColor,
        ),
        shape = RoundedCornerShape(100),
        elevation = ButtonDefaults.buttonElevation(defaultElevation = 0.dp),
        contentPadding = PaddingValues(horizontal = 24.dp)
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.labelLarge.copy(
                fontWeight = FontWeight.SemiBold,
                letterSpacing = 0.5.sp
            )
        )
    }
}
