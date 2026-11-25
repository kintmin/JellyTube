package com.kintmin.presentation.util

import com.kintmin.domain.lyrics.model.LyricsLine
import com.kintmin.domain.lyrics.usecase.nextLyricTimeMs
import com.kintmin.presentation.animation.PLAYBACK_POLLING_INTERVAL_MILLIS
import kotlin.math.ceil
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/**
 * 폴링 사이에 오는 다음 가사 줄을 그 시각에 알린다. 폴링 한 번 안에 오는 줄만 예약하므로,
 * 배속·시크·일시정지는 매 폴링이 다시 예약하며 바로잡고 폴링이 멈추면 예약도 이어지지 않는다.
 */
class NextLyricLineTimer(private val scope: CoroutineScope) {

    private var job: Job? = null

    /** [onReach] 는 그 줄의 시각을 재생 위치로 받는다. */
    fun schedule(
        lines: List<LyricsLine>,
        positionMs: Long,
        playbackSpeed: Float,
        onReach: (positionMs: Long) -> Unit,
    ) {
        cancel()
        val nextMs = nextLyricTimeMs(lines, positionMs) ?: return
        val waitMs = ceil((nextMs - positionMs) / playbackSpeed.toDouble()).toLong()
        if (waitMs > PLAYBACK_POLLING_INTERVAL_MILLIS) return
        job = scope.launch {
            delay(waitMs)
            onReach(nextMs)
        }
    }

    fun cancel() {
        job?.cancel()
        job = null
    }
}
