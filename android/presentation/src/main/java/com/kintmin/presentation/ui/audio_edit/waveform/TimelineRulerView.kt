package com.kintmin.presentation.ui.audio_edit.waveform

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.kintmin.presentation.theme.JellyTubeTheme
import com.kintmin.presentation.theme.bloodyRed60
import com.kintmin.presentation.theme.gray40
import com.kintmin.presentation.theme.gray60
import com.kintmin.presentation.theme.white
import kotlin.math.abs

/** 눈금 간격 후보. 라벨이 겹치지 않을 만큼 넓은 것 중 가장 촘촘한 것을 고른다. */
private val TICK_INTERVAL_MS_LIST = listOf(
    100L, 200L, 500L, 1_000L, 2_000L, 5_000L, 10_000L, 15_000L, 30_000L,
    60_000L, 120_000L, 300_000L, 600_000L, 1_800_000L,
)

/** 눈금 라벨 사이 최소 간격. */
private val MIN_TICK_SPACING = 72.dp

/** 이 거리 안에서 누르면 기준선 대신 끝 표시를 잡는다. */
private val END_MARKER_TOUCH_RADIUS = 24.dp

/**
 * 파형 위의 눈금 줄. 기준선과 끝 표시를 옮기는 영역이다.
 *
 * - 한 손가락으로 누르거나 끌면 기준선이 손가락을 따라온다
 * - 끝 표시 손잡이 근처에서 시작한 끌기는 끝 표시를 옮긴다
 */
@Composable
fun TimelineRulerView(
    modifier: Modifier,
    viewport: TimelineViewportState,
    baselineMs: Long,
    endMs: Long,
    onBaselineChanged: (Long) -> Unit,
    onEndChanged: (Long) -> Unit,
) {
    val textMeasurer = rememberTextMeasurer()
    val currentEndMs by rememberUpdatedState(endMs)
    val currentOnBaselineChanged by rememberUpdatedState(onBaselineChanged)
    val currentOnEndChanged by rememberUpdatedState(onEndChanged)

    Canvas(
        modifier = modifier
            .fillMaxWidth()
            .height(36.dp)
            .onSizeChanged { viewport.updateWidth(it.width.toFloat()) }
            .pointerInput(viewport) {
                awaitEachGesture {
                    val down = awaitFirstDown()
                    val isEndMarker =
                        abs(down.position.x - viewport.xOf(currentEndMs)) <= END_MARKER_TOUCH_RADIUS.toPx()
                    val onMove: (Long) -> Unit = if (isEndMarker) currentOnEndChanged else currentOnBaselineChanged

                    down.consume()
                    // 누르는 순간 기준선이 그 자리로 온다. 끝 표시는 끌어야 움직인다.
                    if (!isEndMarker) onMove(viewport.msAt(down.position.x))

                    while (true) {
                        val change = awaitPointerEvent().changes.firstOrNull { it.id == down.id } ?: break
                        if (!change.pressed) break
                        change.consume()
                        onMove(viewport.msAt(change.position.x))
                    }
                }
            },
    ) {
        val minSpacingMs = MIN_TICK_SPACING.toPx() * viewport.msPerPx
        val intervalMs = TICK_INTERVAL_MS_LIST.firstOrNull { it >= minSpacingMs }
            ?: TICK_INTERVAL_MS_LIST.last()

        // 눈금과 시간 라벨
        var tickMs = (viewport.visibleStartMs.toLong() / intervalMs) * intervalMs
        while (tickMs <= viewport.visibleEndMs) {
            val x = viewport.xOf(tickMs)
            drawRect(
                color = gray60,
                topLeft = Offset(x = x, y = size.height * 0.6f),
                size = Size(width = 1.dp.toPx(), height = size.height * 0.4f),
            )
            drawText(
                textMeasurer = textMeasurer,
                text = formatTickLabel(tickMs, intervalMs),
                topLeft = Offset(x = x + 3.dp.toPx(), y = 2.dp.toPx()),
                style = TextStyle(color = gray40, fontSize = 10.sp),
            )
            tickMs += intervalMs
        }

        // 끝 표시 손잡이
        val endX = viewport.xOf(endMs)
        val handleWidth = 10.dp.toPx()
        drawRect(
            color = bloodyRed60,
            topLeft = Offset(x = endX - handleWidth, y = 0f),
            size = Size(width = handleWidth, height = size.height),
        )

        // 기준선 머리
        val baselineX = viewport.xOf(baselineMs)
        val headSize = 8.dp.toPx()
        drawPath(
            path = Path().apply {
                moveTo(baselineX - headSize, size.height - headSize * 1.5f)
                lineTo(baselineX + headSize, size.height - headSize * 1.5f)
                lineTo(baselineX, size.height)
                close()
            },
            color = white,
        )
    }
}

/** 눈금이 1초보다 촘촘하면 소수 첫째 자리까지 보여준다. */
private fun formatTickLabel(ms: Long, intervalMs: Long): String {
    val totalSeconds = ms / 1_000
    val minutes = totalSeconds / 60
    val seconds = (totalSeconds % 60).toString().padStart(2, '0')
    return if (intervalMs < 1_000) {
        "$minutes:$seconds.${(ms % 1_000) / 100}"
    } else {
        "$minutes:$seconds"
    }
}

@Preview(showBackground = true, backgroundColor = 0xFF191919)
@Composable
fun TimelineRulerViewPreview() {
    JellyTubeTheme {
        val viewport = rememberTimelineViewportState().apply { updateContent(300_000) }
        TimelineRulerView(
            modifier = Modifier,
            viewport = viewport,
            baselineMs = 60_000,
            endMs = 300_000,
            onBaselineChanged = {},
            onEndChanged = {},
        )
    }
}
