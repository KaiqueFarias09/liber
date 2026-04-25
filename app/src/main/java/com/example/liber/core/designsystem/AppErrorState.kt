package com.example.liber.core.designsystem

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.adamglin.PhosphorIcons
import com.adamglin.phosphoricons.Regular
import com.adamglin.phosphoricons.regular.WarningCircle
import com.example.liber.R
import com.example.liber.core.error.AppError
import com.example.liber.core.util.UiText

@Composable
fun AppErrorState(
    error: AppError,
    modifier: Modifier = Modifier,
    onRetry: (() -> Unit)? = null,
    retryLabel: UiText = UiText.StringResource(R.string.action_retry),
    fillMaxSize: Boolean = true,
) {
    AppErrorState(
        title = error.title,
        message = error.message,
        modifier = modifier,
        onRetry = onRetry,
        retryLabel = retryLabel,
        fillMaxSize = fillMaxSize,
    )
}

@Composable
fun AppErrorState(
    message: UiText,
    modifier: Modifier = Modifier,
    title: UiText = UiText.StringResource(R.string.error_default_title),
    onRetry: (() -> Unit)? = null,
    retryLabel: UiText = UiText.StringResource(R.string.action_retry),
    fillMaxSize: Boolean = true,
) {
    Box(
        modifier = modifier
            .then(if (fillMaxSize) Modifier.fillMaxSize() else Modifier.fillMaxWidth())
            .padding(24.dp),
        contentAlignment = Alignment.Center,
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
        ) {
            Box(
                modifier = Modifier
                    .size(56.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.errorContainer),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    imageVector = PhosphorIcons.Regular.WarningCircle,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onErrorContainer,
                    modifier = Modifier.size(28.dp),
                )
            }

            Spacer(Modifier.height(20.dp))

            Text(
                text = title.asString(),
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold),
                color = MaterialTheme.colorScheme.onSurface,
                textAlign = TextAlign.Center,
            )

            Spacer(Modifier.height(8.dp))

            Text(
                text = message.asString(),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
            )

            if (onRetry != null) {
                Spacer(Modifier.height(24.dp))
                LiberButton(
                    text = retryLabel,
                    onClick = onRetry,
                )
            }
        }
    }
}
