package com.kintmin.domain.audio_play_setting

import kotlin.time.Duration
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.Duration.Companion.seconds

/** 잠들기 타이머가 끝날 때 볼륨을 깎는 방법. 두 플랫폼이 같은 곡선을 써야 해서 여기 한 벌만 둔다. */
object SleepTimerFadeOut {

    val DURATION: Duration = 3.seconds

    /** Android 는 볼륨 설정이 프로세스 간 호출이라 더 잘게 쪼개도 들리는 것이 달라지지 않는다. */
    val TICK: Duration = 100.milliseconds

    val TICK_COUNT: Int = (DURATION / TICK).toInt()

    /**
     * 페이드가 [progress](0~1) 만큼 진행됐을 때의 볼륨.
     *
     * 진폭을 선형으로 깎으면 사람 귀에는 끝에서 갑자기 사라진 것처럼 들려 제곱으로 깎는다.
     */
    fun volumeAt(progress: Float): Float {
        val remaining = (1f - progress).coerceIn(0f, 1f)
        return remaining * remaining
    }
}
