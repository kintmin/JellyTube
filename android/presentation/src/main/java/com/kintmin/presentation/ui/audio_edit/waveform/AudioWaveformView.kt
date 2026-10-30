package com.kintmin.presentation.ui.audio_edit.waveform

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.kintmin.domain.audio_edit.model.AudioEditSegment
import com.kintmin.presentation.theme.JellyTubeTheme
import com.kintmin.presentation.theme.bloodyRed60
import com.kintmin.presentation.theme.gray40
import com.kintmin.presentation.theme.seaBlue10
import com.kintmin.presentation.theme.seaBlue100
import com.kintmin.presentation.theme.seaBlue40
import com.kintmin.presentation.theme.white
import kotlin.math.abs

/** 드래그 중인 핸들. */
private enum class SelectionHandle { Start, End }

/**
 * 음원 파형과 편집 상태를 그린다.
 *
 * - 잘라낸 구간은 어둡게 덮어 결과에서 빠지는 것을 보여준다
 * - 선택 구간은 강조하고 양쪽에 드래그 핸들을 둔다
 * - 미리듣기 중에는 현재 위치에 커서를 그린다
 */
@Composable
fun AudioWaveformView(
    modifier: Modifier,
    waveform: List<Int>,
    durationMs: Long,
    selectionStartMs: Long,
    selectionEndMs: Long,
    cutRanges: List<AudioEditSegment>,
    previewPositionMs: Long?,
    onSelectionChanged: (startMs: Long, endMs: Long) -> Unit,
) {
    var widthPx by remember { mutableFloatStateOf(0f) }
    var draggingHandle by remember { mutableStateOf<SelectionHandle?>(null) }

    fun msFromX(x: Float): Long {
        if (widthPx <= 0f || durationMs <= 0L) return 0L
        return (x / widthPx * durationMs).toLong().coerceIn(0L, durationMs)
    }

    fun xFromMs(ms: Long): Float {
        if (durationMs <= 0L) return 0f
        return (ms.toFloat() / durationMs) * widthPx
    }

    Canvas(
        modifier = modifier
            .fillMaxWidth()
            .height(140.dp)
            .onSizeChanged { widthPx = it.width.toFloat() }
            .pointerInput(durationMs) {
                detectDragGestures(
                    onDragStart = { offset ->
                        // 더 가까운 핸들을 집는다
                        val startDistance = abs(offset.x - xFromMs(selectionStartMs))
                        val endDistance = abs(offset.x - xFromMs(selectionEndMs))
                        draggingHandle = if (startDistance <= endDistance) {
                            SelectionHandle.Start
                        } else {
                            SelectionHandle.End
                        }
                    },
                    onDragEnd = { draggingHandle = null },
                    onDragCancel = { draggingHandle = null },
                ) { change, _ ->
                    change.consume()
                    val positionMs = msFromX(change.position.x)
                    when (draggingHandle) {
                        SelectionHandle.Start -> onSelectionChanged(
                            minOf(positionMs, selectionEndMs),
                            selectionEndMs,
                        )

                        SelectionHandle.End -> onSelectionChanged(
                            selectionStartMs,
                            maxOf(positionMs, selectionStartMs),
                        )

                        null -> Unit
                    }
                }
            },
    ) {
        val maxPeak = (waveform.maxOrNull() ?: 0).coerceAtLeast(1)
        val centerY = size.height / 2f

        // 파형 — 버킷이 화면 폭보다 많을 수 있어 픽셀 열마다 해당 구간의 최대값을 쓴다
        if (waveform.isNotEmpty()) {
            val barWidth = 2f
            val barCount = (size.width / barWidth).toInt().coerceAtLeast(1)
            repeat(barCount) { barIndex ->
                val fromIndex = barIndex * waveform.size / barCount
                val toIndex = ((barIndex + 1) * waveform.size / barCount).coerceAtLeast(fromIndex + 1)
                val peak = waveform.subList(fromIndex, toIndex.coerceAtMost(waveform.size)).max()

                val barHeight = (peak.toFloat() / maxPeak) * size.height
                drawRect(
                    color = seaBlue40,
                    topLeft = Offset(x = barIndex * barWidth, y = centerY - barHeight / 2f),
                    size = Size(width = barWidth * 0.7f, height = barHeight),
                )
            }
        } else {
            drawRect(
                color = gray40,
                topLeft = Offset(x = 0f, y = centerY - 1f),
                size = Size(width = size.width, height = 2f),
            )
        }

        // 선택 구간 강조
        val selectionLeft = xFromMs(selectionStartMs)
        val selectionRight = xFromMs(selectionEndMs)
        if (selectionRight > selectionLeft) {
            drawRect(
                color = seaBlue100.copy(alpha = 0.25f),
                topLeft = Offset(x = selectionLeft, y = 0f),
                size = Size(width = selectionRight - selectionLeft, height = size.height),
            )
        }

        // 잘라낸 구간은 결과에서 빠지므로 덮어서 표시
        cutRanges.forEach { cut ->
            val left = xFromMs(cut.startMs)
            val right = xFromMs(cut.endMs)
            drawRect(
                color = bloodyRed60.copy(alpha = 0.45f),
                topLeft = Offset(x = left, y = 0f),
                size = Size(width = (right - left).coerceAtLeast(1f), height = size.height),
            )
        }

        // 선택 핸들
        listOf(selectionLeft, selectionRight).forEach { handleX ->
            drawRect(
                color = seaBlue10,
                topLeft = Offset(x = handleX - 2f, y = 0f),
                size = Size(width = 4f, height = size.height),
            )
        }

        // 미리듣기 커서
        previewPositionMs?.let { positionMs ->
            drawRect(
                color = white,
                topLeft = Offset(x = xFromMs(positionMs) - 1f, y = 0f),
                size = Size(width = 2f, height = size.height),
            )
        }
    }
}

@Preview(showBackground = true, backgroundColor = 0xFF191919)
@Composable
fun AudioWaveformViewPreview() {
    JellyTubeTheme {
        AudioWaveformView(
            modifier = Modifier,
            waveform = List(200) { index -> 3000 + (index * 37 % 29_000) },
            durationMs = 300_000,
            selectionStartMs = 30_000,
            selectionEndMs = 90_000,
            cutRanges = listOf(AudioEditSegment(startMs = 180_000, endMs = 210_000)),
            previewPositionMs = 60_000,
            onSelectionChanged = { _, _ -> },
        )
    }
}

@Preview(showBackground = true, backgroundColor = 0xFF191919)
@Composable
fun AudioWaveformViewEmptyPreview() {
    JellyTubeTheme {
        AudioWaveformView(
            modifier = Modifier,
            waveform = emptyList(),
            durationMs = 300_000,
            selectionStartMs = 0,
            selectionEndMs = 10_000,
            cutRanges = emptyList(),
            previewPositionMs = null,
            onSelectionChanged = { _, _ -> },
        )
    }
}
