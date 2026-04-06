package com.example.liber.ui.components

import androidx.annotation.DrawableRes
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.ColorMatrix
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.example.liber.ui.theme.extendedColors

@Composable
fun EmptyState(
    title: String,
    subtitle: String? = null,
    @DrawableRes image: Int? = null,
    actionLabel: String? = null,
    onAction: (() -> Unit)? = null,
    showImage: Boolean = true,
    containerColor: Color = MaterialTheme.colorScheme.primaryContainer,
    titleColor: Color = MaterialTheme.colorScheme.onPrimaryContainer,
    subtitleColor: Color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.7f),
    modifier: Modifier = Modifier,
) {
    val isDarkTheme = MaterialTheme.extendedColors.isDark
    val invertMatrix = ColorMatrix(
        floatArrayOf(
            -1f, 0f, 0f, 0f, 255f, // Red
            0f, -1f, 0f, 0f, 255f, // Green
            0f, 0f, -1f, 0f, 255f, // Blue
            0f, 0f, 0f, 1f, 0f  // Alpha (keep original)
        )
    )

    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(24.dp))
            .background(containerColor)
            .padding(
                horizontal = 32.dp,
                vertical = if (showImage && image != null) 40.dp else 24.dp
            ),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        if (showImage && image != null) {
            Image(
                painter = painterResource(image),
                contentDescription = null,
                modifier = Modifier.size(220.dp),
                colorFilter = if (isDarkTheme) ColorFilter.colorMatrix(invertMatrix) else null
            )
        }

        Text(
            text = title,
            style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
            color = titleColor,
            textAlign = TextAlign.Center,
        )

        if (subtitle != null) {
            Spacer(Modifier.height(8.dp))
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodyLarge,
                color = subtitleColor,
                textAlign = TextAlign.Center,
            )
        }

        if (actionLabel != null && onAction != null) {
            Spacer(Modifier.height(32.dp))
            LiberButton(
                text = actionLabel,
                onClick = onAction,
                type = LiberButtonType.PRIMARY_INVERTED
            )
        }
    }
}
