package com.kintmin.presentation.ui.custom_ui.floating_component

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Image
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.kintmin.presentation.theme.JellyTubeTheme

@Composable
fun FloatingComponentView(
    modifier: Modifier = Modifier,
    mode: FloatingComponentMode = FloatingComponentMode.Add,
    state: FloatingComponentViewState = rememberFloatingComponentViewState(),
    onTap: (Offset, FloatingComponentViewState) -> Unit = { tapOffset, floatingState ->
        // Debug default: tap to add sample Text/Image blocks.
        val index = floatingState.components.size
        if (index % 2 == 0) {
            floatingState.addComponent(
                touchX = tapOffset.x,
                touchY = tapOffset.y,
                widthPx = 300f,
                heightPx = 300f,
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .clip(RoundedCornerShape(12.dp))
                        .background(Color(0xFFF3F4F6))
                        .border(
                            width = 1.dp,
                            color = Color(0xFF9CA3AF),
                            shape = RoundedCornerShape(12.dp),
                        ),
                ) {
                    Text(
                        text = "Text",
                        modifier = Modifier.align(Alignment.Center),
                    )
                }
            }
        } else {
            floatingState.addComponent(
                touchX = tapOffset.x,
                touchY = tapOffset.y,
                widthPx = 300f,
                heightPx = 300f,
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .clip(RoundedCornerShape(12.dp))
                        .background(Color(0xFFE0F2FE))
                        .border(
                            width = 1.dp,
                            color = Color(0xFF0284C7),
                            shape = RoundedCornerShape(12.dp),
                        ),
                ) {
                    Icon(
                        imageVector = Icons.Rounded.Image,
                        contentDescription = "image component",
                        modifier = Modifier
                            .align(Alignment.Center)
                            .size(48.dp),
                    )
                }
            }
        }
    },
) {
    val density = LocalDensity.current

    Box(
        modifier = modifier
            .clipToBounds()
            .onSizeChanged { state.containerSize = it }
            .pointerInput(mode, state, onTap) {
                detectTapGestures { tapOffset ->
                    if (mode == FloatingComponentMode.Add) {
                        onTap(tapOffset, state)
                    }
                }
            },
    ) {
        state.components.forEach { component ->
            EditableFloatingComponent(
                component = component,
                mode = mode,
                density = density,
                onMove = { dx, dy -> state.moveComponent(component.id, dx, dy) },
                onResize = { handle, dx, dy ->
                    state.resizeComponent(
                        id = component.id,
                        handle = handle,
                        deltaX = dx,
                        deltaY = dy,
                    )
                },
                onRotate = { delta -> state.rotateComponent(component.id, delta) },
            )
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun FloatingComponentViewPreview() {
    JellyTubeTheme {
        FloatingComponentView(
            modifier = Modifier.fillMaxSize(),
            mode = FloatingComponentMode.Edit,
        )
    }
}
