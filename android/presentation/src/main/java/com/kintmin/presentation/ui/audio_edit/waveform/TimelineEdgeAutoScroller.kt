package com.kintmin.presentation.ui.audio_edit.waveform

import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch

/**
 * 드래그 중 손가락이 화면 왼쪽·오른쪽 가장자리에 닿으면 그쪽으로 화면을 계속 밀어 준다.
 *
 * 확대한 채로 기준선·끝 표시·조각 경계·조각을 끌다 보면 목적지가 화면 밖에 있는 경우가 많다.
 * 리스트 재정렬의 자동 스크롤(ReorderState)과 같은 규칙을 쓰되, 손가락을 가만히 대고 있어도
 * 계속 밀리도록 프레임마다 움직인다(리스트는 손가락이 움직일 때만 한 번씩 민다).
 *
 * - 가장자리 구역([zonePx]) 안쪽으로 깊이 들어갈수록 빠르게 민다
 * - 그 방향으로 끌고 있을 때만 민다. 구역 안에서 드래그를 시작했다고 바로 밀리지 않고,
 *   반대쪽으로 손가락을 되돌리면 멈춘다
 * - 화면이 밀릴 때마다 [onScrolled]로 손가락 위치를 다시 알려, 끌던 대상이 손가락 밑을 따라오게 한다
 */
@Stable
class TimelineEdgeAutoScroller(
    private val coroutineScope: CoroutineScope,
    private val viewport: TimelineViewportState,
    private val zonePx: Float,
    private val maxSpeedPxPerSecond: Float,
) {

    private companion object {

        /** 구역 경계에 막 들어왔을 때의 속도 비율. 0이면 들어가자마자는 거의 안 움직여 답답하다. */
        const val MIN_SPEED_RATIO = 0.2f
    }

    private var pointerX = 0f

    /** 최근 손가락 이동 방향. -1 왼쪽, +1 오른쪽, 0 미정. */
    private var dragDirection = 0

    private var onScrolled: ((pointerX: Float) -> Unit)? = null
    private var scrollJob: Job? = null

    fun start(pointerX: Float, onScrolled: (pointerX: Float) -> Unit) {
        this.pointerX = pointerX
        this.dragDirection = 0
        this.onScrolled = onScrolled
    }

    fun update(pointerX: Float) {
        if (pointerX < this.pointerX) dragDirection = -1 else if (pointerX > this.pointerX) dragDirection = 1
        this.pointerX = pointerX

        if (scrollDirection() == 0) {
            scrollJob?.cancel()
        } else if (scrollJob?.isActive != true) {
            scrollJob = coroutineScope.launch { scrollWhileInZone() }
        }
    }

    fun stop() {
        scrollJob?.cancel()
        scrollJob = null
        onScrolled = null
    }

    /** 지금 밀어야 할 방향. -1이면 앞(0초 쪽), +1이면 뒤, 0이면 멈춤. */
    private fun scrollDirection(): Int = when {
        pointerX < zonePx && dragDirection < 0 -> -1
        pointerX > viewport.widthPx - zonePx && dragDirection > 0 -> 1
        else -> 0
    }

    private suspend fun scrollWhileInZone() {
        var lastFrameNanos = withFrameNanos { it }
        while (true) {
            val direction = scrollDirection()
            if (direction == 0) return

            val frameNanos = withFrameNanos { it }
            val elapsedSeconds = (frameNanos - lastFrameNanos) / 1_000_000_000f
            lastFrameNanos = frameNanos

            val depthPx = if (direction < 0) zonePx - pointerX else pointerX - (viewport.widthPx - zonePx)
            val speedRatio = (depthPx / zonePx).coerceIn(MIN_SPEED_RATIO, 1f)
            val stepPx = maxSpeedPxPerSecond * speedRatio * elapsedSeconds

            // pan은 손가락이 화면을 끄는 방향 기준이라, 뒤쪽을 보려면 음수로 민다
            val startMsBefore = viewport.visibleStartMs
            viewport.pan(-direction * stepPx)
            // 0초나 최대 범위에 닿아 더 밀리지 않으면 끌던 대상도 그대로 둔다
            if (viewport.visibleStartMs != startMsBefore) onScrolled?.invoke(pointerX)
        }
    }
}

@Composable
fun rememberTimelineEdgeAutoScroller(
    viewport: TimelineViewportState,
    zone: Dp = 40.dp,
    maxSpeed: Dp = 600.dp,
): TimelineEdgeAutoScroller {
    val density = LocalDensity.current
    val coroutineScope = rememberCoroutineScope()
    return remember(viewport) {
        TimelineEdgeAutoScroller(
            coroutineScope = coroutineScope,
            viewport = viewport,
            zonePx = with(density) { zone.toPx() },
            maxSpeedPxPerSecond = with(density) { maxSpeed.toPx() },
        )
    }
}
