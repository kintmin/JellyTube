package com.kintmin.presentation.ui.audio_edit.waveform

import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.ui.input.pointer.PointerInputScope
import androidx.compose.ui.unit.dp
import kotlin.math.abs

/** 이 거리 안에서 누르면 기준선 대신 끝 표시를 잡는다. */
private val END_MARKER_TOUCH_RADIUS = 24.dp

/** 기준선·끝 표시를 끌다가 달라붙는 자리에서 이 거리 안에 들어오면 그 자리에 붙는다. */
private val SNAP_RADIUS = 12.dp

/**
 * 눈금 줄 제스처.
 *
 * - 한 손가락으로 누르거나 끌면 기준선이 손가락을 따라온다. [baselineSnapTargetsMs] 근처에서는
 *   그 자리에 붙는다. 손가락으로는 0초에 정확히 닿기 어렵다
 * - 끝 표시 손잡이 근처에서 시작한 끌기는 끝 표시를 옮긴다. [endSnapTargetsMs] 근처에서는
 *   그 자리에 붙어, 뒤에 무음도 잘린 소리도 없이 딱 맞추기 쉽다
 */
internal suspend fun PointerInputScope.timelineRulerGestures(
    viewport: TimelineViewportState,
    endMs: () -> Long,
    baselineSnapTargetsMs: () -> List<Long>,
    endSnapTargetsMs: () -> List<Long>,
    onBaselineChanged: (Long) -> Unit,
    onEndChanged: (Long) -> Unit,
) {
    awaitEachGesture {
        val down = awaitFirstDown()
        val isEndMarker = abs(down.position.x - viewport.xOf(endMs())) <= END_MARKER_TOUCH_RADIUS.toPx()

        val snapRadiusPx = SNAP_RADIUS.toPx()
        val moveTo: (Float) -> Unit = if (isEndMarker) {
            fun(x: Float) { onEndChanged(viewport.snapMsAt(x, endSnapTargetsMs(), snapRadiusPx)) }
        } else {
            fun(x: Float) { onBaselineChanged(viewport.snapMsAt(x, baselineSnapTargetsMs(), snapRadiusPx)) }
        }

        down.consume()
        // 누르는 순간 기준선이 그 자리로 온다. 끝 표시는 끌어야 움직인다.
        if (!isEndMarker) moveTo(down.position.x)

        while (true) {
            val change = awaitPointerEvent().changes.firstOrNull { it.id == down.id } ?: break
            if (!change.pressed) break
            change.consume()
            moveTo(change.position.x)
        }
    }
}
