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
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp

@Composable
fun EmptyState(
    title: String,
    subtitle: String? = null,
    @DrawableRes image: Int? = null,
    actionLabel: String? = null,
    onAction: (() -> Unit)? = null,
    containerColor: Color = MaterialTheme.colorScheme.primaryContainer,
    titleColor: Color = MaterialTheme.colorScheme.onPrimaryContainer,
    subtitleColor: Color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.7f),
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(20.dp))
            .background(containerColor)
            .padding(horizontal = 32.dp, vertical = 40.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        if (image != null) {
            Image(
                painter = painterResource(image),
                contentDescription = null,
                modifier = Modifier.size(250.dp),
            )
        }

        Text(
            text = title,
            style = MaterialTheme.typography.titleMedium,
            color = titleColor,
            textAlign = TextAlign.Center,
        )

        if (subtitle != null) {
            Spacer(Modifier.height(8.dp))
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodyMedium,
                color = subtitleColor,
                textAlign = TextAlign.Center,
            )
        }

        if (actionLabel != null && onAction != null) {
            Spacer(Modifier.height(24.dp))
            Button(onClick = onAction) {
                Text(actionLabel)
            }
        }
    }
}
