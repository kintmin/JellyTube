package com.kintmin.presentation.ui.custom_ui.floating_component

import androidx.compose.foundation.layout.BoxScope
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.unit.IntSize
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.sin

@Composable
fun rememberFloatingComponentViewState(): FloatingComponentViewState {
    return remember { FloatingComponentViewState() }
}

@Stable
class FloatingComponentViewState {
    internal var containerSize by mutableStateOf(IntSize.Zero)
    internal val components = mutableStateListOf<FloatingComponent>()

    private var idSeed by mutableLongStateOf(0L)
    private val minComponentWidthPx = 48f
    private val minComponentHeightPx = 48f
    private val snapAngles = floatArrayOf(-180f, -90f, 0f, 90f, 180f)
    private val snapEnterThresholdDeg = 1f
    private val snapExitThresholdDeg = 2f

    private data class Placement(
        val left: Float,
        val top: Float,
        val width: Float,
        val height: Float,
    )

    fun clearComponents() {
        components.clear()
    }

    fun addComponent(
        touchX: Float,
        touchY: Float,
        widthPx: Float,
        heightPx: Float,
        content: @Composable BoxScope.() -> Unit,
    ): FloatingComponent? {
        val containerWidth = containerSize.width.toFloat()
        val containerHeight = containerSize.height.toFloat()
        if (containerWidth <= 0f || containerHeight <= 0f) return null

        val targetWidth = widthPx.coerceAtLeast(1f).coerceAtMost(containerWidth)
        val targetHeight = heightPx.coerceAtLeast(1f).coerceAtMost(containerHeight)
        val adjustedLeft = touchX.coerceIn(0f, containerWidth - targetWidth)
        val adjustedTop = touchY.coerceIn(0f, containerHeight - targetHeight)

        val component = FloatingComponent(
            id = idSeed++,
            leftPx = adjustedLeft,
            topPx = adjustedTop,
            widthPx = targetWidth,
            heightPx = targetHeight,
            rotationDeg = 0f,
            content = content,
        )
        components.add(component)
        return component
    }

    fun moveComponent(
        id: Long,
        deltaX: Float,
        deltaY: Float,
    ) {
        val index = components.indexOfFirst { it.id == id }
        if (index < 0) return

        val component = components[index]
        val containerWidth = containerSize.width.toFloat()
        val containerHeight = containerSize.height.toFloat()
        if (containerWidth <= 0f || containerHeight <= 0f) return

        // Keep rotated bounds inside container while moving.
        val constrained = constrainPlacementForRotation(
            left = component.leftPx + deltaX,
            top = component.topPx + deltaY,
            width = component.widthPx,
            height = component.heightPx,
            rotationDeg = component.rotationDeg,
            containerWidth = containerWidth,
            containerHeight = containerHeight,
        )

        components[index] = component.copy(
            leftPx = constrained.left,
            topPx = constrained.top,
        )
    }

    fun rotateComponent(
        id: Long,
        deltaDeg: Float,
    ) {
        val index = components.indexOfFirst { it.id == id }
        if (index < 0) return

        val component = components[index]
        val rawRotation = normalizeDegree(component.rotationDeg + deltaDeg)
        val newRotation = applySnapDetent(
            previousRotation = component.rotationDeg,
            rawRotation = rawRotation,
        )

        // Rotation keeps size fixed. Only center is clamped.
        val constrained = constrainPlacementKeepSizeForRotation(
            left = component.leftPx,
            top = component.topPx,
            width = component.widthPx,
            height = component.heightPx,
            rotationDeg = newRotation,
            containerWidth = containerSize.width.toFloat(),
            containerHeight = containerSize.height.toFloat(),
        ) ?: return

        components[index] = component.copy(
            leftPx = constrained.left,
            topPx = constrained.top,
            rotationDeg = newRotation,
        )
    }

    internal fun resizeComponent(
        id: Long,
        handle: ResizeHandle,
        deltaX: Float,
        deltaY: Float,
    ) {
        val index = components.indexOfFirst { it.id == id }
        if (index < 0) return

        val component = components[index]
        val containerWidth = containerSize.width.toFloat()
        val containerHeight = containerSize.height.toFloat()
        if (containerWidth <= 0f || containerHeight <= 0f) return

        // Delta is already produced in rotated UI space by handle gestures.
        val localDeltaX = deltaX
        val localDeltaY = deltaY

        val oldWidth = component.widthPx
        val oldHeight = component.heightPx
        val ratio = safeRatio(oldWidth, oldHeight)

        // Local axis bounds around center (before resize).
        var minX = -oldWidth / 2f
        var maxX = oldWidth / 2f
        var minY = -oldHeight / 2f
        var maxY = oldHeight / 2f

        when (handle) {
            ResizeHandle.Left -> {
                minX += localDeltaX
                if (maxX - minX < minComponentWidthPx) minX = maxX - minComponentWidthPx
            }
            ResizeHandle.Right -> {
                maxX += localDeltaX
                if (maxX - minX < minComponentWidthPx) maxX = minX + minComponentWidthPx
            }
            ResizeHandle.Top -> {
                minY += localDeltaY
                if (maxY - minY < minComponentHeightPx) minY = maxY - minComponentHeightPx
            }
            ResizeHandle.Bottom -> {
                maxY += localDeltaY
                if (maxY - minY < minComponentHeightPx) maxY = minY + minComponentHeightPx
            }
            ResizeHandle.TopLeft -> {
                val widthPriority = abs(localDeltaX / oldWidth) >= abs(localDeltaY / oldHeight)
                if (widthPriority) {
                    val newWidth = (oldWidth - localDeltaX).coerceAtLeast(minComponentWidthPx)
                    val newHeight = (newWidth / ratio).coerceAtLeast(minComponentHeightPx)
                    minX = maxX - newWidth
                    minY = maxY - newHeight
                } else {
                    val newHeight = (oldHeight - localDeltaY).coerceAtLeast(minComponentHeightPx)
                    val newWidth = (newHeight * ratio).coerceAtLeast(minComponentWidthPx)
                    minX = maxX - newWidth
                    minY = maxY - newHeight
                }
            }
            ResizeHandle.TopRight -> {
                val widthPriority = abs(localDeltaX / oldWidth) >= abs(localDeltaY / oldHeight)
                if (widthPriority) {
                    val newWidth = (oldWidth + localDeltaX).coerceAtLeast(minComponentWidthPx)
                    val newHeight = (newWidth / ratio).coerceAtLeast(minComponentHeightPx)
                    maxX = minX + newWidth
                    minY = maxY - newHeight
                } else {
                    val newHeight = (oldHeight - localDeltaY).coerceAtLeast(minComponentHeightPx)
                    val newWidth = (newHeight * ratio).coerceAtLeast(minComponentWidthPx)
                    maxX = minX + newWidth
                    minY = maxY - newHeight
                }
            }
            ResizeHandle.BottomLeft -> {
                val widthPriority = abs(localDeltaX / oldWidth) >= abs(localDeltaY / oldHeight)
                if (widthPriority) {
                    val newWidth = (oldWidth - localDeltaX).coerceAtLeast(minComponentWidthPx)
                    val newHeight = (newWidth / ratio).coerceAtLeast(minComponentHeightPx)
                    minX = maxX - newWidth
                    maxY = minY + newHeight
                } else {
                    val newHeight = (oldHeight + localDeltaY).coerceAtLeast(minComponentHeightPx)
                    val newWidth = (newHeight * ratio).coerceAtLeast(minComponentWidthPx)
                    minX = maxX - newWidth
                    maxY = minY + newHeight
                }
            }
            ResizeHandle.BottomRight -> {
                val widthPriority = abs(localDeltaX / oldWidth) >= abs(localDeltaY / oldHeight)
                if (widthPriority) {
                    val newWidth = (oldWidth + localDeltaX).coerceAtLeast(minComponentWidthPx)
                    val newHeight = (newWidth / ratio).coerceAtLeast(minComponentHeightPx)
                    maxX = minX + newWidth
                    maxY = minY + newHeight
                } else {
                    val newHeight = (oldHeight + localDeltaY).coerceAtLeast(minComponentHeightPx)
                    val newWidth = (newHeight * ratio).coerceAtLeast(minComponentWidthPx)
                    maxX = minX + newWidth
                    maxY = minY + newHeight
                }
            }
        }

        val newWidth = (maxX - minX).coerceAtLeast(minComponentWidthPx)
        val newHeight = (maxY - minY).coerceAtLeast(minComponentHeightPx)
        val localCenterShiftX = (minX + maxX) / 2f
        val localCenterShiftY = (minY + maxY) / 2f

        // Convert local center shift back to screen shift.
        val (screenShiftX, screenShiftY) = localToScreenVector(
            localX = localCenterShiftX,
            localY = localCenterShiftY,
            rotationDeg = component.rotationDeg,
        )

        val oldCenterX = component.leftPx + oldWidth / 2f
        val oldCenterY = component.topPx + oldHeight / 2f
        val left = (oldCenterX + screenShiftX) - newWidth / 2f
        val top = (oldCenterY + screenShiftY) - newHeight / 2f

        // Final safety clamp in container.
        val constrained = constrainPlacementForRotation(
            left = left,
            top = top,
            width = newWidth,
            height = newHeight,
            rotationDeg = component.rotationDeg,
            containerWidth = containerWidth,
            containerHeight = containerHeight,
        )

        components[index] = component.copy(
            leftPx = constrained.left,
            topPx = constrained.top,
            widthPx = constrained.width,
            heightPx = constrained.height,
        )
    }

    private fun normalizeDegree(deg: Float): Float {
        var normalized = deg % 360f
        if (normalized > 180f) normalized -= 360f
        if (normalized < -180f) normalized += 360f
        return normalized
    }

    private fun applySnapDetent(
        previousRotation: Float,
        rawRotation: Float,
    ): Float {
        val prevSnap = nearestSnap(previousRotation)
        val rawSnap = nearestSnap(rawRotation)

        val prevFromSnap = abs(shortestAngleDeltaDeg(prevSnap, previousRotation))
        val rawFromPrevSnap = abs(shortestAngleDeltaDeg(prevSnap, rawRotation))
        val rawFromRawSnap = abs(shortestAngleDeltaDeg(rawSnap, rawRotation))

        val isPreviouslySnapped = prevFromSnap < 0.001f
        if (isPreviouslySnapped && rawFromPrevSnap <= snapExitThresholdDeg) return prevSnap
        if (rawFromRawSnap <= snapEnterThresholdDeg) return rawSnap
        return rawRotation
    }

    private fun nearestSnap(angle: Float): Float {
        var nearest = snapAngles[0]
        var minDistance = abs(shortestAngleDeltaDeg(nearest, angle))
        for (i in 1 until snapAngles.size) {
            val candidate = snapAngles[i]
            val distance = abs(shortestAngleDeltaDeg(candidate, angle))
            if (distance < minDistance) {
                minDistance = distance
                nearest = candidate
            }
        }
        return nearest
    }

    private fun safeRatio(width: Float, height: Float): Float {
        if (height <= 0f) return 1f
        return (width / height).coerceIn(0.01f, 100f)
    }

    private fun localToScreenVector(
        localX: Float,
        localY: Float,
        rotationDeg: Float,
    ): Pair<Float, Float> {
        val rad = Math.toRadians(rotationDeg.toDouble())
        val c = cos(rad).toFloat()
        val s = sin(rad).toFloat()
        val screenX = localX * c - localY * s
        val screenY = localX * s + localY * c
        return screenX to screenY
    }

    private fun constrainPlacementForRotation(
        left: Float,
        top: Float,
        width: Float,
        height: Float,
        rotationDeg: Float,
        containerWidth: Float,
        containerHeight: Float,
    ): Placement {
        if (containerWidth <= 0f || containerHeight <= 0f) {
            return Placement(left, top, width, height)
        }

        var w = width.coerceAtLeast(1f).coerceAtMost(containerWidth)
        var h = height.coerceAtLeast(1f).coerceAtMost(containerHeight)

        // Ensure rotated footprint can fit in container.
        val (bboxWidth, bboxHeight) = rotatedBoundsSize(w, h, rotationDeg)
        if (bboxWidth > containerWidth || bboxHeight > containerHeight) {
            val scaleW = if (bboxWidth > 0f) containerWidth / bboxWidth else 1f
            val scaleH = if (bboxHeight > 0f) containerHeight / bboxHeight else 1f
            val scale = minOf(scaleW, scaleH, 1f)
            w = (w * scale).coerceAtLeast(1f).coerceAtMost(containerWidth)
            h = (h * scale).coerceAtLeast(1f).coerceAtMost(containerHeight)
        }

        val (halfExtentX, halfExtentY) = rotatedHalfExtents(w, h, rotationDeg)
        val targetCenterX = left + w / 2f
        val targetCenterY = top + h / 2f
        val clampedCenterX = clampCenter(
            value = targetCenterX,
            min = halfExtentX,
            max = containerWidth - halfExtentX,
            fallback = containerWidth / 2f,
        )
        val clampedCenterY = clampCenter(
            value = targetCenterY,
            min = halfExtentY,
            max = containerHeight - halfExtentY,
            fallback = containerHeight / 2f,
        )

        return Placement(
            left = clampedCenterX - w / 2f,
            top = clampedCenterY - h / 2f,
            width = w,
            height = h,
        )
    }

    private fun constrainPlacementKeepSizeForRotation(
        left: Float,
        top: Float,
        width: Float,
        height: Float,
        rotationDeg: Float,
        containerWidth: Float,
        containerHeight: Float,
    ): Placement? {
        if (containerWidth <= 0f || containerHeight <= 0f) {
            return Placement(left, top, width, height)
        }

        val (bboxWidth, bboxHeight) = rotatedBoundsSize(width, height, rotationDeg)
        if (bboxWidth > containerWidth || bboxHeight > containerHeight) return null

        val (halfExtentX, halfExtentY) = rotatedHalfExtents(width, height, rotationDeg)
        val targetCenterX = left + width / 2f
        val targetCenterY = top + height / 2f
        val clampedCenterX = targetCenterX.coerceIn(halfExtentX, containerWidth - halfExtentX)
        val clampedCenterY = targetCenterY.coerceIn(halfExtentY, containerHeight - halfExtentY)

        return Placement(
            left = clampedCenterX - width / 2f,
            top = clampedCenterY - height / 2f,
            width = width,
            height = height,
        )
    }

    private fun rotatedBoundsSize(
        width: Float,
        height: Float,
        rotationDeg: Float,
    ): Pair<Float, Float> {
        val rad = Math.toRadians(rotationDeg.toDouble())
        val c = abs(cos(rad)).toFloat()
        val s = abs(sin(rad)).toFloat()
        return (width * c + height * s) to (width * s + height * c)
    }

    private fun rotatedHalfExtents(
        width: Float,
        height: Float,
        rotationDeg: Float,
    ): Pair<Float, Float> {
        val (bboxWidth, bboxHeight) = rotatedBoundsSize(width, height, rotationDeg)
        return bboxWidth / 2f to bboxHeight / 2f
    }

    private fun clampCenter(
        value: Float,
        min: Float,
        max: Float,
        fallback: Float,
    ): Float {
        return if (min <= max) value.coerceIn(min, max) else fallback
    }
}
