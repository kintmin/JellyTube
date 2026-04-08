package com.kintmin.presentation.ui.custom_ui.image_drawing

import android.view.MotionEvent
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.Undo
import androidx.compose.material.icons.rounded.Brush
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.CompositingStrategy
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInteropFilter
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.kintmin.presentation.theme.JellyTubeTheme
import java.io.File

@Composable
fun ImageDrawingView(
    modifier: Modifier,
    imageModel: Any?,
    isEraserMode: Boolean,
    onEraserModeChange: (Boolean) -> Unit,
    onEffectiveEraserModeChange: (Boolean) -> Unit = {},
    brushColor: Color = Color.Red,
    brushWidth: Float = 12f,
    eraserWidth: Float = 32f,
) {
    val strokes = remember { mutableStateListOf<DrawStroke>() }

    var currentPath by remember { mutableStateOf<Path?>(null) }
    var currentPathStart by remember { mutableStateOf(Offset.Zero) }
    var currentPathVersion by remember { mutableIntStateOf(0) }
    var eraserCursor by remember { mutableStateOf<Offset?>(null) }
    var stylusButtonPressed by remember { mutableStateOf(false) }
    var currentStrokeIsEraser by remember { mutableStateOf(false) }

    val effectiveEraserMode = isEraserMode || stylusButtonPressed

    LaunchedEffect(effectiveEraserMode) {
        onEffectiveEraserModeChange(effectiveEraserMode)
    }

    Column(modifier = modifier) {
        ToolBarRow(
            isEraserMode = isEraserMode,
            onToggleMode = { onEraserModeChange(!isEraserMode) },
            onUndo = {
                if (strokes.isNotEmpty()) {
                    strokes.removeAt(strokes.lastIndex)
                }
            },
            onClear = { strokes.clear() },
        )

        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 12.dp, vertical = 8.dp)
                .background(
                    color = MaterialTheme.colorScheme.surface,
                    shape = RoundedCornerShape(8.dp),
                )
        ) {
            val context = LocalContext.current

            AsyncImage(
                model = if (imageModel is String && imageModel.isNotEmpty()) {
                    ImageRequest.Builder(context)
                        .data(File(imageModel))
                        .build()
                } else {
                    imageModel
                },
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize(),
            )

            Canvas(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Transparent)
                    .pointerInteropFilter { event ->
                        stylusButtonPressed = isStylusPrimaryButtonPressed(event)

                        val effectiveEraserForEvent = isEraserMode || stylusButtonPressed

                        when (event.actionMasked) {
                            MotionEvent.ACTION_DOWN -> {
                                currentPathStart = Offset(event.x, event.y)
                                currentStrokeIsEraser = effectiveEraserForEvent
                                currentPath = Path().apply {
                                    moveTo(event.x, event.y)
                                }
                                if (currentStrokeIsEraser) {
                                    eraserCursor = Offset(event.x, event.y)
                                }
                                currentPathVersion++
                                true
                            }

                            MotionEvent.ACTION_MOVE -> {
                                currentPath?.let { path ->
                                    for (i in 0 until event.historySize) {
                                        path.lineTo(event.getHistoricalX(i), event.getHistoricalY(i))
                                    }
                                    path.lineTo(event.x, event.y)
                                    if (currentStrokeIsEraser) {
                                        eraserCursor = Offset(event.x, event.y)
                                    }
                                    currentPathVersion++
                                }
                                true
                            }

                            MotionEvent.ACTION_UP -> {
                                currentPath?.let { path ->
                                    val completedPath = Path().apply { addPath(path) }
                                    strokes.add(
                                        DrawStroke(
                                            path = completedPath,
                                            color = brushColor,
                                            width = if (currentStrokeIsEraser) eraserWidth else brushWidth,
                                            isEraser = currentStrokeIsEraser,
                                        )
                                    )
                                }
                                currentPath = null
                                currentStrokeIsEraser = false
                                eraserCursor = null
                                currentPathVersion++
                                true
                            }

                            MotionEvent.ACTION_CANCEL -> {
                                currentPath = null
                                currentStrokeIsEraser = false
                                eraserCursor = null
                                currentPathVersion++
                                true
                            }

                            else -> false
                        }
                    }
                    .graphicsLayer(
                        compositingStrategy = CompositingStrategy.Offscreen,
                    )
            ) {
                currentPathVersion

                strokes.forEach { stroke ->
                    drawPath(
                        path = stroke.path,
                        color = if (stroke.isEraser) Color.Transparent else stroke.color,
                        style = Stroke(
                            width = stroke.width,
                            cap = StrokeCap.Round,
                            join = StrokeJoin.Round,
                        ),
                        blendMode = if (stroke.isEraser) BlendMode.Clear else BlendMode.SrcOver,
                    )
                }

                currentPath?.let { path ->
                    val currentWidth = if (currentStrokeIsEraser) eraserWidth else brushWidth
                    if (path.isEmpty) {
                        drawCircle(
                            color = if (currentStrokeIsEraser) Color.Transparent else brushColor,
                            radius = currentWidth / 2f,
                            center = currentPathStart,
                            blendMode = if (currentStrokeIsEraser) BlendMode.Clear else BlendMode.SrcOver,
                        )
                    } else {
                        drawPath(
                            path = path,
                            color = if (currentStrokeIsEraser) Color.Transparent else brushColor,
                            style = Stroke(
                                width = currentWidth,
                                cap = StrokeCap.Round,
                                join = StrokeJoin.Round,
                            ),
                            blendMode = if (currentStrokeIsEraser) BlendMode.Clear else BlendMode.SrcOver,
                        )
                    }
                }

                if (currentStrokeIsEraser) {
                    eraserCursor?.let { cursor ->
                        val radius = eraserWidth / 2f
                        drawCircle(
                            color = Color.White.copy(alpha = 0.18f),
                            radius = radius,
                            center = cursor,
                        )
                        drawCircle(
                            color = Color.White.copy(alpha = 0.9f),
                            radius = radius,
                            center = cursor,
                            style = Stroke(width = 2.dp.toPx()),
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun ToolBarRow(
    isEraserMode: Boolean,
    onToggleMode: () -> Unit,
    onUndo: () -> Unit,
    onClear: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 8.dp, vertical = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Row {
            IconButton(onClick = onToggleMode) {
                Icon(
                    imageVector = Icons.Rounded.Brush,
                    contentDescription = "모드 변경",
                    tint = if (isEraserMode) MaterialTheme.colorScheme.outline else MaterialTheme.colorScheme.primary,
                )
            }
            Text(
                text = if (isEraserMode) "지우개" else "펜",
                modifier = Modifier.padding(top = 14.dp),
                color = MaterialTheme.colorScheme.onSurface,
            )
        }

        Row {
            IconButton(onClick = onUndo) {
                Icon(
                    imageVector = Icons.AutoMirrored.Rounded.Undo,
                    contentDescription = "실행취소",
                )
            }
            IconButton(onClick = onClear) {
                Icon(
                    imageVector = Icons.Rounded.Delete,
                    contentDescription = "전체 삭제",
                )
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
fun ImageDrawingViewPreview() {
    var previewEraserMode by remember { mutableStateOf(false) }

    JellyTubeTheme {
        ImageDrawingView(
            modifier = Modifier.fillMaxSize(),
            imageModel = null,
            isEraserMode = previewEraserMode,
            onEraserModeChange = { previewEraserMode = it },
        )
    }
}

private data class DrawStroke(
    val path: Path,
    val color: Color,
    val width: Float,
    val isEraser: Boolean,
)

private fun isStylusPrimaryButtonPressed(event: MotionEvent): Boolean {
    val pointerIndex = event.actionIndex
    if (pointerIndex < 0 || pointerIndex >= event.pointerCount) return false

    return event.getToolType(pointerIndex) == MotionEvent.TOOL_TYPE_STYLUS &&
        event.buttonState == MotionEvent.BUTTON_STYLUS_PRIMARY
}
