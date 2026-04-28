package com.kintmin.presentation.animation

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.foundation.Canvas
import androidx.compose.material3.MaterialTheme
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.lerp
import kotlin.math.abs
import kotlin.math.hypot
import kotlin.math.sin

@Composable
fun FocusSpreadOverlay(
    modifier: Modifier = Modifier,
    progress: Float,
) {
    val primary = MaterialTheme.colorScheme.primary
    val accent = MaterialTheme.colorScheme.tertiary
    Canvas(modifier = modifier) {
        val clamped = progress.coerceIn(0f, 1f)
        val envelope = (1f - abs((2f * clamped) - 1f)).coerceIn(0f, 1f)
        if (envelope <= 0f) return@Canvas

        val width = size.width
        val height = size.height
        val center = Offset(width * 0.5f, height * 0.54f)
        val diagonal = hypot(width, height)
        val radius = diagonal * (0.24f + (0.84f * clamped))

        drawCircle(
            brush = Brush.radialGradient(
                colors = listOf(
                    primary.copy(alpha = 0.15f * envelope),
                    accent.copy(alpha = 0.08f * envelope),
                    Color.Transparent,
                ),
                center = center,
                radius = radius,
            ),
            radius = radius,
            center = center,
        )
        val scaleRadius = height * 0.20f
        val rowStep = scaleRadius * 1.18f
        val colStep = scaleRadius * 1.86f
        val phase = clamped * TAU * 2.2f
        val drift = width * (0.10f + (0.16f * clamped))
        val scalePath = Path()

        var rowIndex = 0
        var y = -scaleRadius
        while (y <= height + scaleRadius) {
            val rowOffset = if (rowIndex % 2 == 0) 0f else scaleRadius * 0.93f
            var x = (-2f * scaleRadius) + rowOffset + drift
            while (x <= width + (2f * scaleRadius)) {
                val wave = ((sin((x / width) * TAU * 1.8f + (y / height) * TAU * 0.7f + phase) + 1f) * 0.5f)
                    .coerceIn(0f, 1f)
                val alpha = (0.05f + (0.22f * wave)) * envelope
                val tint = lerp(primary, accent, wave)

                scalePath.reset()
                scalePath.moveTo(x - scaleRadius, y)
                scalePath.quadraticTo(x, y - (scaleRadius * 0.90f), x + scaleRadius, y)
                scalePath.quadraticTo(x, y + (scaleRadius * 0.56f), x - scaleRadius, y)
                scalePath.close()

                drawPath(
                    path = scalePath,
                    color = tint.copy(alpha = alpha),
                )
                x += colStep
            }
            y += rowStep
            rowIndex += 1
        }
    }
}

private const val TAU = 6.2831855f
