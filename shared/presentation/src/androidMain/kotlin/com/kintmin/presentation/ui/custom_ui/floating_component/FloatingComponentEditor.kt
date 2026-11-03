package com.kintmin.presentation.ui.custom_ui.floating_component

import android.view.MotionEvent
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.input.pointer.pointerInteropFilter
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import kotlin.math.roundToInt

@Composable
internal fun EditableFloatingComponent(
    component: FloatingComponent,
    mode: FloatingComponentMode,
    density: Density,
    onMove: (Float, Float) -> Unit,
    onResize: (ResizeHandle, Float, Float) -> Unit,
    onRotate: (Float) -> Unit,
) {
    var componentCenterInWindow by remember { mutableStateOf(Offset.Zero) }

    val frameModifier = Modifier
        .offset {
            IntOffset(
                x = component.leftPx.roundToInt(),
                y = component.topPx.roundToInt(),
            )
        }
        .size(
            width = with(density) { component.widthPx.toDp() },
            height = with(density) { component.heightPx.toDp() },
        )
        .onGloballyPositioned { coordinates ->
            componentCenterInWindow = coordinates.localToWindow(
                Offset(
                    x = coordinates.size.width / 2f,
                    y = coordinates.size.height / 2f,
                )
            )
        }

    Box(modifier = frameModifier) {
        if (mode == FloatingComponentMode.Edit) {
            // Move gesture stays in non-rotated parent space.
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .pointerInput(mode, component.id) {
                        detectDragGestures { change, dragAmount ->
                            change.consume()
                            onMove(dragAmount.x, dragAmount.y)
                        }
                    },
            )
        }

        Box(
            modifier = Modifier
                .fillMaxSize()
                .graphicsLayer {
                    rotationZ = component.rotationDeg
                    transformOrigin = TransformOrigin(0.5f, 0.5f)
                },
        ) {
            Box(
                modifier = Modifier.fillMaxSize(),
                content = component.content,
            )

            if (mode == FloatingComponentMode.Edit) {
                RotationHandleView(
                    componentCenterInWindow = componentCenterInWindow,
                    onRotate = onRotate,
                )

                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .border(1.dp, Color(0xFF22C55E), RoundedCornerShape(8.dp)),
                ) {
                    ResizeHandleView(Alignment.CenterStart, ResizeHandle.Left, onResize)
                    ResizeHandleView(Alignment.CenterEnd, ResizeHandle.Right, onResize)
                    ResizeHandleView(Alignment.TopCenter, ResizeHandle.Top, onResize)
                    ResizeHandleView(Alignment.BottomCenter, ResizeHandle.Bottom, onResize)
                    ResizeHandleView(Alignment.TopStart, ResizeHandle.TopLeft, onResize)
                    ResizeHandleView(Alignment.TopEnd, ResizeHandle.TopRight, onResize)
                    ResizeHandleView(Alignment.BottomStart, ResizeHandle.BottomLeft, onResize)
                    ResizeHandleView(Alignment.BottomEnd, ResizeHandle.BottomRight, onResize)
                }
            }
        }
    }
}

@Composable
private fun BoxScope.ResizeHandleView(
    alignment: Alignment,
    handle: ResizeHandle,
    onResize: (ResizeHandle, Float, Float) -> Unit,
) {
    val latestOnResize by rememberUpdatedState(onResize)

    Box(
        modifier = Modifier
            .align(alignment)
            .size(18.dp)
            .clip(RoundedCornerShape(999.dp))
            .background(Color.White)
            .border(1.dp, Color(0xFF16A34A), RoundedCornerShape(999.dp))
            .pointerInput(handle) {
                detectDragGestures { change, dragAmount ->
                    change.consume()
                    latestOnResize(handle, dragAmount.x, dragAmount.y)
                }
            },
    )
}

@Composable
private fun BoxScope.RotationHandleView(
    componentCenterInWindow: Offset,
    onRotate: (Float) -> Unit,
) {
    val latestOnRotate by rememberUpdatedState(onRotate)
    var previousAngleDeg by remember { mutableStateOf<Float?>(null) }

    Box(
        modifier = Modifier
            .align(Alignment.TopCenter)
            .offset(y = (-28).dp)
            .size(20.dp)
            .clip(RoundedCornerShape(999.dp))
            .background(Color(0xFFDBEAFE))
            .border(1.dp, Color(0xFF2563EB), RoundedCornerShape(999.dp))
            .pointerInteropFilter { event ->
                when (event.actionMasked) {
                    MotionEvent.ACTION_DOWN -> {
                        previousAngleDeg = angleDeg(
                            center = componentCenterInWindow,
                            point = Offset(event.rawX, event.rawY),
                        )
                        true
                    }

                    MotionEvent.ACTION_MOVE -> {
                        val prev = previousAngleDeg ?: return@pointerInteropFilter false
                        val currentAngle = angleDeg(
                            center = componentCenterInWindow,
                            point = Offset(event.rawX, event.rawY),
                        )
                        latestOnRotate(shortestAngleDeltaDeg(prev, currentAngle))
                        previousAngleDeg = currentAngle
                        true
                    }

                    MotionEvent.ACTION_UP,
                    MotionEvent.ACTION_CANCEL -> {
                        previousAngleDeg = null
                        true
                    }

                    else -> false
                }
            },
    )
}
