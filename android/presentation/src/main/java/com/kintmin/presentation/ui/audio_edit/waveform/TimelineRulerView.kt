package com.kintmin.presentation.ui.audio_edit.waveform

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material3.MaterialTheme
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.kintmin.presentation.extension.to_hh_colon_mm_colon_ss
import com.kintmin.presentation.theme.JellyTubeTheme
import com.kintmin.presentation.theme.bloodyRed100
import com.kintmin.presentation.theme.gray40
import com.kintmin.presentation.theme.gray60
import com.kintmin.presentation.theme.white
import kotlin.math.abs
import kotlin.time.Duration.Companion.milliseconds

/** 눈금 간격 후보. 라벨이 겹치지 않을 만큼 넓은 것 중 가장 촘촘한 것을 고른다. */
private val TICK_INTERVAL_MS_LIST = listOf(
    100L, 200L, 500L, 1_000L, 2_000L, 5_000L, 10_000L, 15_000L, 30_000L,
    60_000L, 120_000L, 300_000L, 600_000L, 1_800_000L,
)

/** 눈금 라벨 사이 최소 간격. */
private val MIN_TICK_SPACING = 72.dp

/** 이 거리 안에서 누르면 기준선 대신 끝 표시를 잡는다. */
private val END_MARKER_TOUCH_RADIUS = 24.dp

/** 끝 표시를 끌다가 소리 끝에서 이 거리 안에 들어오면 그 자리에 붙는다. */
private val END_MARKER_SNAP_RADIUS = 12.dp

/**
 * 파형 위의 눈금 줄. 기준선과 끝 표시를 옮기는 영역이다.
 *
 * - 한 손가락으로 누르거나 끌면 기준선이 손가락을 따라온다
 * - 끝 표시 손잡이 근처에서 시작한 끌기는 끝 표시를 옮긴다. 소리 끝([snapEndMs]) 근처에서는
 *   그 자리에 붙어, 뒤에 무음도 잘린 소리도 없이 딱 맞추기 쉽다
 */
@Composable
fun TimelineRulerView(
    modifier: Modifier,
    viewport: TimelineViewportState,
    baselineMs: Long,
    endMs: Long,
    /** 끝 표시가 달라붙는 위치. 마지막 조각이 끝나는 곳이다. 조각이 없으면 null. */
    snapEndMs: Long?,
    onBaselineChanged: (Long) -> Unit,
    onEndChanged: (Long) -> Unit,
) {
    val textMeasurer = rememberTextMeasurer()
    // 끝 라벨 뒤를 배경색으로 덮어 눈금 라벨과 겹쳐도 읽히게 한다
    val backgroundColor = MaterialTheme.colorScheme.background
    val currentEndMs by rememberUpdatedState(endMs)
    val currentSnapEndMs by rememberUpdatedState(snapEndMs)
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

                    down.consume()
                    // 누르는 순간 기준선이 그 자리로 온다. 끝 표시는 끌어야 움직인다.
                    if (!isEndMarker) currentOnBaselineChanged(viewport.msAt(down.position.x))

                    while (true) {
                        val change = awaitPointerEvent().changes.firstOrNull { it.id == down.id } ?: break
                        if (!change.pressed) break
                        change.consume()

                        val x = change.position.x
                        if (isEndMarker) {
                            // 확대 정도와 상관없이 손에 같은 범위로 느껴지도록 화면 거리로 잰다
                            val snapMs = currentSnapEndMs
                                ?.takeIf { abs(viewport.xOf(it) - x) <= END_MARKER_SNAP_RADIUS.toPx() }
                            currentOnEndChanged(snapMs ?: viewport.msAt(x))
                        } else {
                            currentOnBaselineChanged(viewport.msAt(x))
                        }
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

        // 끝 표시 손잡이 — 조각 손잡이처럼 세로 줄을 넣어 잡아 끄는 곳으로 보이게 한다
        val endX = viewport.xOf(endMs)
        val handleWidth = 12.dp.toPx()
        drawRect(
            color = bloodyRed100,
            topLeft = Offset(x = endX - handleWidth, y = 0f),
            size = Size(width = handleWidth, height = size.height),
        )
        val gripLineHeight = size.height * 0.5f
        listOf(endX - handleWidth * 0.65f, endX - handleWidth * 0.35f).forEach { lineX ->
            drawRect(
                color = white.copy(alpha = 0.8f),
                topLeft = Offset(x = lineX - 0.5.dp.toPx(), y = (size.height - gripLineHeight) / 2f),
                size = Size(width = 1.dp.toPx(), height = gripLineHeight),
            )
        }

        // 끝 라벨 — 빨간 손잡이가 결과물의 끝이고 지금 몇 초인지 손잡이 자체가 말하게 한다.
        // 손잡이 왼쪽에 두되, 화면 왼쪽 밖으로 나가면 오른쪽에 붙인다.
        val endLabel = textMeasurer.measure(
            text = "끝 ${endMs.milliseconds.to_hh_colon_mm_colon_ss()}",
            style = TextStyle(color = bloodyRed100, fontSize = 11.sp, fontWeight = FontWeight.Bold),
        )
        val labelGap = 4.dp.toPx()
        val labelLeftX = endX - handleWidth - labelGap - endLabel.size.width
        val labelX = if (labelLeftX >= 0f) labelLeftX else endX + labelGap
        val labelY = (size.height * 0.6f - endLabel.size.height).coerceAtLeast(0f)
        drawRect(
            color = backgroundColor,
            topLeft = Offset(x = labelX - 2.dp.toPx(), y = labelY),
            size = Size(width = endLabel.size.width + 4.dp.toPx(), height = endLabel.size.height.toFloat()),
        )
        drawText(textLayoutResult = endLabel, topLeft = Offset(x = labelX, y = labelY))

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
            snapEndMs = 300_000,
            onBaselineChanged = {},
            onEndChanged = {},
        )
    }
}
