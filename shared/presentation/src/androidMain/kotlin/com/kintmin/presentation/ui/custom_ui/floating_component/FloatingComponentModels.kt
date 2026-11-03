package com.kintmin.presentation.ui.custom_ui.floating_component

import androidx.compose.foundation.layout.BoxScope
import androidx.compose.runtime.Composable

enum class FloatingComponentMode {
    Add,
    Edit,
}

internal enum class ResizeHandle {
    Left,
    Right,
    Top,
    Bottom,
    TopLeft,
    TopRight,
    BottomLeft,
    BottomRight,
}

data class FloatingComponent(
    val id: Long,
    val leftPx: Float,
    val topPx: Float,
    val widthPx: Float,
    val heightPx: Float,
    val rotationDeg: Float,
    val content: @Composable BoxScope.() -> Unit,
)
