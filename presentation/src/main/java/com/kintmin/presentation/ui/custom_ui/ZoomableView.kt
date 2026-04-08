package com.kintmin.presentation.ui.custom_ui

import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.material3.Text
import com.kintmin.presentation.theme.JellyTubeTheme
import kotlin.math.max

enum class ZoomLimitMode {
    Horizontal,
    Vertical,
    Auto,
}

enum class ZoomContentAlignment {
    Start,
    Middle,
    End,
}

@Composable
fun ZoomableView(
    modifier: Modifier = Modifier,
    zoomLimitMode: ZoomLimitMode = ZoomLimitMode.Auto,
    contentAlignment: ZoomContentAlignment = ZoomContentAlignment.Start,
    minScale: Float = 1f,
    maxScale: Float = minScale * 4f,
    doubleTapScale: Float = minScale * 2f,
    content: @Composable BoxScope.() -> Unit,
) {
    var scale by remember { mutableFloatStateOf(minScale) }
    var offsetX by remember { mutableFloatStateOf(0f) }
    var offsetY by remember { mutableFloatStateOf(0f) }
    var containerSize by remember { mutableStateOf(IntSize.Zero) }
    var contentSize by remember { mutableStateOf(IntSize.Zero) }
    var initializedLayoutKey by remember { mutableStateOf<String?>(null) }

    val horizontalFitMinScale = if (containerSize.width > 0 && contentSize.width > 0) {
        containerSize.width.toFloat() / contentSize.width.toFloat()
    } else {
        minScale
    }
    val verticalFitMinScale = if (containerSize.height > 0 && contentSize.height > 0) {
        containerSize.height.toFloat() / contentSize.height.toFloat()
    } else {
        minScale
    }

    val resolvedLimitMode = when (zoomLimitMode) {
        ZoomLimitMode.Auto -> {
            if (contentSize.width >= contentSize.height) ZoomLimitMode.Horizontal
            else ZoomLimitMode.Vertical
        }
        else -> zoomLimitMode
    }

    val effectiveMinScale = when (resolvedLimitMode) {
        ZoomLimitMode.Horizontal -> horizontalFitMinScale
        ZoomLimitMode.Vertical -> verticalFitMinScale
        ZoomLimitMode.Auto -> minScale
    }
    val effectiveMaxScale = max(maxScale, effectiveMinScale)

    fun alignedOffset(remainingSpace: Float): Float {
        return when (contentAlignment) {
            ZoomContentAlignment.Start -> 0f
            ZoomContentAlignment.Middle -> remainingSpace / 2f
            ZoomContentAlignment.End -> remainingSpace
        }
    }

    fun clampOffsetX(rawTranslation: Float, currentScale: Float): Float {
        if (contentSize.width == 0 || containerSize.width == 0) return 0f

        val scaledWidth = contentSize.width * currentScale
        val overflow = scaledWidth - containerSize.width
        if (overflow <= 0f) {
            val remainingSpace = containerSize.width - scaledWidth
            return alignedOffset(remainingSpace = remainingSpace)
        }
        return rawTranslation.coerceIn(-overflow, 0f)
    }

    fun clampOffsetY(rawTranslation: Float, currentScale: Float): Float {
        if (contentSize.height == 0 || containerSize.height == 0) return 0f

        val scaledHeight = contentSize.height * currentScale
        val overflow = scaledHeight - containerSize.height
        if (overflow <= 0f) {
            val remainingSpace = containerSize.height - scaledHeight
            return alignedOffset(remainingSpace = remainingSpace)
        }
        return rawTranslation.coerceIn(-overflow, 0f)
    }

    if (resolvedLimitMode == ZoomLimitMode.Horizontal || resolvedLimitMode == ZoomLimitMode.Vertical) {
        val hasLayoutInfo = containerSize.width > 0 &&
            containerSize.height > 0 &&
            contentSize.width > 0 &&
            contentSize.height > 0
        if (hasLayoutInfo) {
            val layoutKey = "${resolvedLimitMode.name}|${containerSize.width}x${containerSize.height}|${contentSize.width}x${contentSize.height}"
            if (initializedLayoutKey != layoutKey) {
                SideEffect {
                    initializedLayoutKey = layoutKey
                    scale = effectiveMinScale
                    offsetX = clampOffsetX(rawTranslation = 0f, currentScale = effectiveMinScale)
                    offsetY = clampOffsetY(rawTranslation = 0f, currentScale = effectiveMinScale)
                }
            }
        }
    }

    fun applyScale(targetScale: Float, focus: Offset) {
        val newScale = targetScale.coerceIn(effectiveMinScale, effectiveMaxScale)
        val scaleDelta = newScale / scale

        val dx = focus.x * (1f - scaleDelta)
        val dy = focus.y * (1f - scaleDelta)

        val rawX = offsetX + dx
        val rawY = offsetY + dy

        scale = newScale
        offsetX = clampOffsetX(rawTranslation = rawX, currentScale = newScale)
        offsetY = clampOffsetY(rawTranslation = rawY, currentScale = newScale)
    }

    Box(
        modifier = modifier
            .clipToBounds()
            .onSizeChanged { containerSize = it }
            .pointerInput(effectiveMinScale, effectiveMaxScale, contentSize, containerSize) {
                detectTransformGestures { centroid, pan, zoom, _ ->
                    val newScale = (scale * zoom).coerceIn(effectiveMinScale, effectiveMaxScale)
                    val scaleDelta = newScale / scale

                    val scaleDx = centroid.x * (1f - scaleDelta)
                    val scaleDy = centroid.y * (1f - scaleDelta)
                    val rawX = offsetX + pan.x + scaleDx
                    val rawY = offsetY + pan.y + scaleDy

                    scale = newScale
                    offsetX = clampOffsetX(rawTranslation = rawX, currentScale = newScale)
                    offsetY = clampOffsetY(rawTranslation = rawY, currentScale = newScale)
                }
            }
            .pointerInput(effectiveMinScale, effectiveMaxScale, doubleTapScale) {
                detectTapGestures(
                    onDoubleTap = { offset ->
                        val targetScale = if (scale > effectiveMinScale) effectiveMinScale else doubleTapScale
                        applyScale(targetScale = targetScale, focus = offset)
                    }
                )
            }
    ) {
        Box(
            modifier = Modifier
                .wrapContentSize(align = Alignment.TopStart, unbounded = true)
                .onSizeChanged { contentSize = it }
                .graphicsLayer {
                    scaleX = scale
                    scaleY = scale
                    translationX = offsetX
                    translationY = offsetY
                    transformOrigin = androidx.compose.ui.graphics.TransformOrigin(0f, 0f)
                },
            content = content,
        )
    }
}

@Preview(showBackground = true)
@Composable
fun ZoomableViewPreview() {
    JellyTubeTheme {
        ZoomableView(
            modifier = Modifier.fillMaxSize(),
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color(0xFFE8F2FF)),
                contentAlignment = Alignment.Center,
            ) {
                Text(text = "Pinch / Drag / Double Tap")
            }
        }
    }
}
