package com.kintmin.domain.audio_play_setting

import org.junit.Assert.assertEquals
import org.junit.Test
import kotlin.time.Duration
import kotlin.time.Duration.Companion.minutes
import kotlin.time.Duration.Companion.seconds
import kotlin.time.TestTimeSource

class SleepTimerScheduleTest {

    private val timeSource = TestTimeSource()

    private fun schedule(total: Duration) = SleepTimerSchedule(timeSource.markNow(), total)

    @Test
    fun `시작 직후에는 설정한 시간이 그대로 남는다`() {
        assertEquals(30.minutes, schedule(30.minutes).remaining())
    }

    @Test
    fun `흐른 만큼 남은 시간이 줄어든다`() {
        val schedule = schedule(30.minutes)
        timeSource += 10.minutes
        assertEquals(20.minutes, schedule.remaining())
    }

    @Test
    fun `설정한 시간을 넘겨도 남은 시간이 0 밑으로 내려가지 않는다`() {
        val schedule = schedule(30.minutes)
        timeSource += 31.minutes
        assertEquals(Duration.ZERO, schedule.remaining())
    }

    @Test
    fun `페이드는 끝나기 3초 전에 시작한다`() {
        assertEquals(30.minutes - 3.seconds, schedule(30.minutes).delayUntilFadeOut())
    }

    @Test
    fun `설정한 시간이 페이드 길이보다 짧으면 기다리지 않고 바로 페이드한다`() {
        assertEquals(Duration.ZERO, schedule(2.seconds).delayUntilFadeOut())
    }

    @Test
    fun `이미 페이드 구간에 들어와 있으면 더 기다리지 않는다`() {
        val schedule = schedule(30.minutes)
        timeSource += 30.minutes - 1.seconds
        assertEquals(Duration.ZERO, schedule.delayUntilFadeOut())
    }
}
