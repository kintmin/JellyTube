package com.kintmin.presentation.ui.common

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.kintmin.presentation.platform.PlatformFullScreenDialog

@Composable
fun JellyTubeDialog(
    showDialog: Boolean,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier,
    dismissOnClickOutside: Boolean = true,
    shape: Shape = RoundedCornerShape(16.dp),
    scrimColor: Color = MaterialTheme.colorScheme.scrim.copy(alpha = 0.32f),
    containerColor: Color = MaterialTheme.colorScheme.surface,
    tonalElevation: Dp = 0.dp,
    surfaceModifier: Modifier = Modifier
        .fillMaxWidth()
        .padding(24.dp),
    content: @Composable () -> Unit,
) {
    if (!showDialog) return

    PlatformFullScreenDialog(onDismissRequest = onDismiss) {
        val interactionSource = remember { MutableInteractionSource() }

        Box(
            modifier = modifier
                .fillMaxSize()
                .background(scrimColor)
                .clickable(
                    interactionSource = interactionSource,
                    indication = null,
                    onClick = {
                        if (dismissOnClickOutside) onDismiss()
                    },
                ),
            contentAlignment = Alignment.Center,
        ) {
            Surface(
                modifier = surfaceModifier.clickable(
                    interactionSource = interactionSource,
                    indication = null,
                    onClick = {},
                ),
                shape = shape,
                color = containerColor,
                tonalElevation = tonalElevation,
            ) {
                content()
            }
        }
    }
}
