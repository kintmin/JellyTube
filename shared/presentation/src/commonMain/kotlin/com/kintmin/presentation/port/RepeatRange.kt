package com.kintmin.presentation.port

import kotlin.time.Duration

/**
 * A-B 구간반복 상태.
 *
 * 플랫폼 구현이 각자 들면 두 플랫폼이 다른 답을 낼 수 있어 여기 한 벌만 둔다.
 * 플레이어가 주는 기능이 아니라 앱이 직접 쓴 로직이라 공통 코드에 있다.
 */
class RepeatRange {

    private var heldMediaId: String? = null
    private var start: Duration? = null
    private var end: Duration? = null

    fun stateFor(playingMediaId: String?): RepeatRangeState {
        clearIfMediaChanged(playingMediaId)
        return RepeatRangeState(mediaId = heldMediaId, startDuration = start, endDuration = end)
    }

    /** 누를 때마다 시작점 → 끝점 → 해제로 돈다. */
    fun update(playingMediaId: String, currentDuration: Duration): Result<Unit> = runCatching {
        clearIfMediaChanged(playingMediaId)

        when {
            start == null -> {
                heldMediaId = playingMediaId
                start = currentDuration
            }

            end == null -> {
                val startDuration = start ?: return@runCatching
                check(currentDuration > startDuration)
                end = currentDuration
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
}
