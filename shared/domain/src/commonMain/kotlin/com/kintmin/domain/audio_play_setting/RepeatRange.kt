package com.kintmin.domain.audio_play_setting

import com.kintmin.domain.audio_play_setting.model.RepeatRangeState
import kotlin.time.Duration
import kotlin.time.Duration.Companion.seconds

/**
 * A-B 구간반복 상태.
 *
 * 플레이어가 주는 기능이 아니라 앱이 직접 쓴 로직이다.
 */
class RepeatRange {

    private var heldMediaId: String? = null

    private var start: Duration? = null
    private var end: Duration? = null

    fun stateFor(playingMediaId: String?): RepeatRangeState {
        clearIfMediaChanged(playingMediaId)
        return RepeatRangeState(mediaId = heldMediaId, startDuration = start, endDuration = end)
    }

    /**
     * 누를 때마다 시작점 → 끝점 → 해제로 돈다.
     *
     * 시작점을 잡은 자리에서 곧바로 다시 누르면 구간을 잡는 대신 **푼다** — 잘못 눌러 되돌리려는
     * 쪽에 가깝고, 그만한 길이는 구간으로 쓸 수도 없다. 시작점보다 앞에서 누른 경우도 같다.
     * 그러지 않으면 잘못 잡은 시작점을 그 앞에서는 풀 길이 없다.
     */
    fun update(playingMediaId: String, currentDuration: Duration) {
        clearIfMediaChanged(playingMediaId)

        when {
            start == null -> {
                heldMediaId = playingMediaId
                start = currentDuration
            }

            end == null -> {
                val startDuration = start ?: return
                if (currentDuration - startDuration < CANCEL_WINDOW) clear() else end = currentDuration
            }

            else -> clear()
        }
    }

    /** 끝점을 넘었으면 돌아갈 시작점. */
    fun rewindTarget(playingMediaId: String, currentDuration: Duration): Duration? {
        clearIfMediaChanged(playingMediaId)

        val startDuration = start ?: return null
        val endDuration = end ?: return null
        return startDuration.takeIf { currentDuration >= endDuration }
    }

    fun clear() {
        heldMediaId = null
        start = null
        end = null
    }

    /** 길이가 바뀌면 옛 길이 기준으로 잡아둔 구간은 더 이상 유효하지 않다. */
    fun clearIfHeldBy(mediaId: String) {
        if (heldMediaId == mediaId) clear()
    }

    private fun clearIfMediaChanged(playingMediaId: String?) {
        if (heldMediaId != null && heldMediaId != playingMediaId) clear()
    }

    companion object {
        /**
         * 시작점에서 이만큼 안에 다시 누르면 같은 자리로 본다. 재생 중에는 누를 때마다 위치가
         * 조금씩 흘러서, 정확히 같은 값이 되는 때는 멈춰 있을 때뿐이다.
         */
        private val CANCEL_WINDOW = 1.seconds
    }
}
