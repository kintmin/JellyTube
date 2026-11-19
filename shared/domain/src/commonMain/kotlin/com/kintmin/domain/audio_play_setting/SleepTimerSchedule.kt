package com.kintmin.domain.audio_play_setting

import kotlin.time.Duration
import kotlin.time.TimeMark

/**
 * 잠들기 타이머의 남은 시간.
 *
 * 벽시계가 아니라 단조시계([TimeMark])를 쓴다. 기기 시계가 바뀌거나 보정되면 표시만 튀고
 * 실제 만료는 그대로여서 둘이 어긋난다.
 *
 * 재생 중인 음원이 무엇인지는 쥐지 않는다 — 곡이 바뀌어도 타이머는 살아 있어야 한다.
 */
class SleepTimerSchedule(
    private val startMark: TimeMark,
    val total: Duration,
) {

    fun remaining(): Duration = (total - startMark.elapsedNow()).coerceAtLeast(Duration.ZERO)

    /** 페이드를 시작하기까지 기다릴 시간. 남은 시간이 페이드 길이보다 짧으면 기다리지 않는다. */
    fun delayUntilFadeOut(): Duration =
        (remaining() - SleepTimerFadeOut.DURATION).coerceAtLeast(Duration.ZERO)
}
