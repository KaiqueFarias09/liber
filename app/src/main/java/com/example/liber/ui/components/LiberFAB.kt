package com.example.liber.ui.components

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.FloatingActionButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SmallFloatingActionButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp

enum class LiberFabSize { STANDARD, SMALL }

/**
 * A standardized Floating Action Button.
 *
 * - STANDARD: 60×60dp, 22dp corner radius — matches the reference Soft Material design.
 * - SMALL: 40×40dp, 14dp corner radius — useful for secondary actions.
 *
 * Container defaults to primary (Rose) with onPrimary content.
 */
@Composable
fun LiberFAB(
    onClick: () -> Unit,
    icon: ImageVector,
    modifier: Modifier = Modifier,
    contentDescription: String? = null,
    size: LiberFabSize = LiberFabSize.STANDARD,
    containerColor: Color = MaterialTheme.colorScheme.primary,
    contentColor: Color = MaterialTheme.colorScheme.onPrimary,
) {
    when (size) {
        LiberFabSize.STANDARD -> {
            FloatingActionButton(
                onClick = onClick,
                modifier = modifier.size(60.dp),
                containerColor = containerColor,
                contentColor = contentColor,
                shape = RoundedCornerShape(22.dp),
                elevation = FloatingActionButtonDefaults.elevation(
                    defaultElevation = 4.dp,
                    pressedElevation = 2.dp,
                ),
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = icon,
                        contentDescription = contentDescription,
                    )
                }
            }
        }

        LiberFabSize.SMALL -> {
            SmallFloatingActionButton(
                onClick = onClick,
                modifier = modifier.size(40.dp),
                containerColor = containerColor,
                contentColor = contentColor,
                shape = RoundedCornerShape(14.dp),
                elevation = FloatingActionButtonDefaults.elevation(
                    defaultElevation = 3.dp,
                    pressedElevation = 1.dp,
                ),
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = icon,
                        contentDescription = contentDescription,
                        modifier = Modifier.size(18.dp),
                    )
                }
            }
        }
    }
}
