package com.kintmin.presentation.ui.custom_ui

import androidx.compose.ui.geometry.Offset
import kotlin.math.atan2

internal fun angleDeg(center: Offset, point: Offset): Float {
    return Math.toDegrees(
        atan2(
            y = (point.y - center.y).toDouble(),
            x = (point.x - center.x).toDouble(),
        )
    ).toFloat()
}

internal fun shortestAngleDeltaDeg(from: Float, to: Float): Float {
    var delta = to - from
    while (delta > 180f) delta -= 360f
    while (delta < -180f) delta += 360f
    return delta
}
