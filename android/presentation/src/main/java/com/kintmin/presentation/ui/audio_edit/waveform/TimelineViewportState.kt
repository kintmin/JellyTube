package com.kintmin.presentation.ui.audio_edit.waveform

import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue

/**
 * 눈금 줄과 파형이 함께 쓰는 화면 범위(어디부터 얼마나 확대해서 보는지).
 *
 * 화면에만 필요한 상태라 ViewModel이 아니라 Composable에 둔다. 제스처마다 바뀌어서
 * UiState에 넣으면 매 프레임 상태가 복사된다.
 */
@Stable
class TimelineViewportState {

    private companion object {

        /** 최대로 확대했을 때 화면 폭에 보이는 길이. */
        const val MIN_VISIBLE_MS = 1_000f

        /** 최대로 축소했을 때 보이는 길이(전체 길이 배수). 오른쪽에 여유 공간이 생긴다. */
        const val MAX_VISIBLE_RATIO = 3f

        /**
         * 첫 화면의 오른쪽 여유(전체 길이 비율). 끝 표시가 화면 끝에 붙으면 뒤로 끌 수 없고
         * 시스템 뒤로 가기 제스처와도 겹친다.
         */
        const val INITIAL_RIGHT_MARGIN_RATIO = 0.15f

        /** 미리듣기선을 따라 화면을 넘길 때, 선을 화면 왼쪽에서 이만큼(화면 폭 비율) 띄워 둔다. */
        const val FOLLOW_LEFT_MARGIN_RATIO = 0.05f
    }

    var widthPx by mutableFloatStateOf(0f)
        private set

    /** 원본과 결과물 끝 중 긴 쪽. 첫 화면 맞춤과 확대/이동 한계의 기준이다. */
    private var contentMs by mutableLongStateOf(0L)

    /** 화면 왼쪽 끝의 시각. */
    var visibleStartMs by mutableFloatStateOf(0f)
        private set

    /** 1px이 나타내는 시간. 작을수록 확대된 상태다. */
    var msPerPx by mutableFloatStateOf(1f)
        private set

    private var isFitted = false

    /** 재생 중 사용자가 직접 화면을 움직이면 따라가기를 멈춘다. 손가락과 자동 이동이 싸우지 않게 한다. */
    private var isFollowPaused = false

    val visibleEndMs: Float get() = visibleStartMs + widthPx * msPerPx

    fun updateWidth(widthPx: Float) {
        this.widthPx = widthPx
        fitIfNeeded()
    }

    fun updateContent(contentMs: Long) {
        this.contentMs = contentMs
        fitIfNeeded()
    }

    /** 처음 한 번만 전체가 화면 폭에 들어오도록 맞춘다. 오른쪽에는 끝 표시를 끌 여유를 둔다. */
    private fun fitIfNeeded() {
        if (isFitted || widthPx <= 0f || contentMs <= 0L) return
        isFitted = true
        msPerPx = contentMs * (1f + INITIAL_RIGHT_MARGIN_RATIO) / widthPx
        visibleStartMs = 0f
    }

    fun xOf(ms: Long): Float = (ms - visibleStartMs) / msPerPx

    fun msAt(x: Float): Long = (visibleStartMs + x * msPerPx).toLong()

    /** 손가락이 [dxPx]만큼 움직였을 때 화면을 따라 옮긴다. */
    fun pan(dxPx: Float) {
        isFollowPaused = true
        visibleStartMs = (visibleStartMs - dxPx * msPerPx).coerceIn(0f, maxStartMs())
    }

    /** 손가락 사이 중심([focusX])이 가리키는 시각을 고정한 채 [zoom]배 확대한다. */
    fun zoom(zoom: Float, focusX: Float) {
        if (widthPx <= 0f || zoom <= 0f) return
        isFollowPaused = true
        val focusMs = visibleStartMs + focusX * msPerPx
        val maxVisibleMs = (contentMs * MAX_VISIBLE_RATIO).coerceAtLeast(MIN_VISIBLE_MS)
        msPerPx = (msPerPx / zoom).coerceIn(MIN_VISIBLE_MS / widthPx, maxVisibleMs / widthPx)
        visibleStartMs = (focusMs - focusX * msPerPx).coerceIn(0f, maxStartMs())
    }

    /** 미리듣기를 새로 시작하면 다시 따라간다. */
    fun resumeFollow() {
        isFollowPaused = false
    }

    /**
     * 미리듣기선 [positionMs]가 화면 밖으로 나가면 한 화면 넘긴다.
     *
     * 사용자가 화면을 옮겨 따라가기가 멈춘 상태면 넘기지 않는다. 선이 다시 화면 안으로 들어오면
     * 사용자가 그 자리를 보고 있다는 뜻이라 다시 따라간다.
     */
    fun follow(positionMs: Long) {
        if (widthPx <= 0f) return
        if (positionMs >= visibleStartMs && positionMs <= visibleEndMs) {
            isFollowPaused = false
            return
        }
        if (isFollowPaused) return

        val leftMarginMs = widthPx * msPerPx * FOLLOW_LEFT_MARGIN_RATIO
        visibleStartMs = (positionMs - leftMarginMs).coerceIn(0f, maxStartMs())
    }

    /** 0초 앞은 보여주지 않고, 오른쪽은 최대 축소 범위까지만 이동한다. */
    private fun maxStartMs(): Float =
        (contentMs * MAX_VISIBLE_RATIO - widthPx * msPerPx).coerceAtLeast(0f)
}

@Composable
fun rememberTimelineViewportState(): TimelineViewportState = remember { TimelineViewportState() }
