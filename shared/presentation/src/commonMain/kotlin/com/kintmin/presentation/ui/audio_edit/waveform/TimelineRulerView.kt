package com.kintmin.presentation.ui.audio_edit.waveform

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.ContentCut
import androidx.compose.material.icons.rounded.Merge
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.drawscope.translate
import androidx.compose.ui.graphics.vector.rememberVectorPainter
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
import com.kintmin.presentation.extension.to_hh_colon_mm_colon_ss_dot_sss
import com.kintmin.presentation.theme.black
import com.kintmin.presentation.theme.JellyTubeTheme
import com.kintmin.presentation.theme.bloodyRed100
import com.kintmin.presentation.theme.gray40
import com.kintmin.presentation.theme.gray60
import com.kintmin.presentation.theme.white
import kotlin.time.Duration.Companion.milliseconds

/** 눈금 간격 후보. 라벨이 겹치지 않을 만큼 넓은 것 중 가장 촘촘한 것을 고른다. */
private val TICK_INTERVAL_MS_LIST = listOf(
    100L, 200L, 500L, 1_000L, 2_000L, 5_000L, 10_000L, 15_000L, 30_000L,
    60_000L, 120_000L, 300_000L, 600_000L, 1_800_000L,
)

/** 눈금 라벨 사이 최소 간격. */
private val MIN_TICK_SPACING = 72.dp

@Composable
fun TimelineRulerView(
    modifier: Modifier,
    viewport: TimelineViewportState,
    baselineMs: Long,
    endMs: Long,
    canSplit: Boolean,
    canMerge: Boolean,
    /** 기준선이 달라붙는 위치들. */
    baselineSnapTargetsMs: List<Long>,
    /** 끝 표시가 달라붙는 위치들. */
    endSnapTargetsMs: List<Long>,
    onBaselineChanged: (Long) -> Unit,
    onEndChanged: (Long) -> Unit,
) {
    val textMeasurer = rememberTextMeasurer()
    val cutPainter = rememberVectorPainter(Icons.Rounded.ContentCut)
    val mergePainter = rememberVectorPainter(Icons.Rounded.Merge)
    // 끝 라벨 뒤를 배경색으로 덮어 눈금 라벨과 겹쳐도 읽히게 한다
    val backgroundColor = MaterialTheme.colorScheme.background
    val currentEndMs by rememberUpdatedState(endMs)
    val currentBaselineSnapTargetsMs by rememberUpdatedState(baselineSnapTargetsMs)
    val currentEndSnapTargetsMs by rememberUpdatedState(endSnapTargetsMs)
    val autoScroller = rememberTimelineEdgeAutoScroller(viewport)
    val currentOnBaselineChanged by rememberUpdatedState(onBaselineChanged)
    val currentOnEndChanged by rememberUpdatedState(onEndChanged)

    Canvas(
        modifier = modifier
            .fillMaxWidth()
            .height(36.dp)
            .onSizeChanged { viewport.updateWidth(it.width.toFloat()) }
            .pointerInput(viewport) {
                timelineRulerGestures(
                    viewport = viewport,
                    autoScroller = autoScroller,
                    endMs = { currentEndMs },
                    baselineSnapTargetsMs = { currentBaselineSnapTargetsMs },
                    endSnapTargetsMs = { currentEndSnapTargetsMs },
                    onBaselineChanged = { ms -> currentOnBaselineChanged(ms) },
                    onEndChanged = { ms -> currentOnEndChanged(ms) },
                )
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

        // 끝 표시 손잡이 — 꽉 찬 세로 막대는 재생 위치로 읽혀 `]` 모양으로 그린다
        val endX = viewport.xOf(endMs)
        val barWidth = 3.dp.toPx()
        val armLength = 8.dp.toPx()
        drawRect(
            color = bloodyRed100,
            topLeft = Offset(x = endX - barWidth, y = 0f),
            size = Size(width = barWidth, height = size.height),
        )
        listOf(0f, size.height - barWidth).forEach { armY ->
            drawRect(
                color = bloodyRed100,
                topLeft = Offset(x = endX - armLength, y = armY),
                size = Size(width = armLength, height = barWidth),
            )
        }

        // 끝 라벨 — 빨간 손잡이가 결과물의 끝이고 지금 몇 초인지 손잡이 자체가 말하게 한다.
        // 손잡이 왼쪽에 두되, 화면 왼쪽 밖으로 나가면 오른쪽에 붙인다.
        val endLabel = textMeasurer.measure(
            text = "끝 ${endMs.milliseconds.to_hh_colon_mm_colon_ss()}",
            style = TextStyle(color = bloodyRed100, fontSize = 11.sp, fontWeight = FontWeight.Bold),
        )
        val labelGap = 4.dp.toPx()
        val labelLeftX = endX - armLength - labelGap - endLabel.size.width
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
        val baselineLabel = textMeasurer.measure(
            text = baselineMs.milliseconds.to_hh_colon_mm_colon_ss_dot_sss(),
            style = TextStyle(color = black, fontSize = 11.sp, fontWeight = FontWeight.Bold),
        )
        val actionPainter = when {
            canMerge -> mergePainter
            canSplit -> cutPainter
            else -> null
        }
        val iconSize = 12.dp.toPx()
        val bubblePadding = 6.dp.toPx()
        val iconGap = 3.dp.toPx()
        val bubbleHeight = baselineLabel.size.height + 4.dp.toPx()
        val bubbleWidth = bubblePadding * 2 + baselineLabel.size.width + if (actionPainter != null) iconSize + iconGap else 0f
        // 기준선이 화면 밖에 있어도 시각은 읽히도록 가장자리에 붙인다
        val bubbleLeft = (baselineX - bubbleWidth / 2f).coerceIn(0f, (size.width - bubbleWidth).coerceAtLeast(0f))
        drawRect(
            color = white,
            topLeft = Offset(x = baselineX - 1.dp.toPx(), y = bubbleHeight),
            size = Size(width = 2.dp.toPx(), height = size.height - bubbleHeight),
        )
        drawRoundRect(
            color = white,
            topLeft = Offset(x = bubbleLeft, y = 0f),
            size = Size(width = bubbleWidth, height = bubbleHeight),
            cornerRadius = CornerRadius(bubbleHeight / 2f),
        )
        var contentX = bubbleLeft + bubblePadding
        actionPainter?.let { painter ->
            translate(left = contentX, top = (bubbleHeight - iconSize) / 2f) {
                with(painter) { draw(size = Size(iconSize, iconSize), colorFilter = ColorFilter.tint(black)) }
            }
            contentX += iconSize + iconGap
        }
        drawText(
            textLayoutResult = baselineLabel,
            topLeft = Offset(x = contentX, y = (bubbleHeight - baselineLabel.size.height) / 2f),
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

@Preview
@Composable
fun TimelineRulerViewPreview() {
    JellyTubeTheme {
        val viewport = rememberTimelineViewportState().apply { updateContent(300_000) }
        TimelineRulerView(
            modifier = Modifier,
            viewport = viewport,
            baselineMs = 60_000,
            endMs = 300_000,
            canSplit = true,
            canMerge = false,
            baselineSnapTargetsMs = listOf(0L, 300_000L),
            endSnapTargetsMs = listOf(300_000L),
            onBaselineChanged = {},
            onEndChanged = {},
        )
    }
}
