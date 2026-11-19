package com.kintmin.presentation.sleep_timer

import com.kintmin.domain.audio_media.model.AudioMedia
import com.kintmin.domain.audio_play_setting.SleepTimerFadeOut
import com.kintmin.presentation.port.AudioPreviewPlayer
import com.kintmin.presentation.port.PlaybackController
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import io.mockk.verifyOrder
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import kotlinx.datetime.LocalDateTime
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.time.Duration
import kotlin.time.Duration.Companion.minutes
import kotlin.time.Duration.Companion.seconds
import kotlin.time.TestTimeSource

@OptIn(ExperimentalCoroutinesApi::class)
class SleepTimerRunnerTest {

    private val volumes = mutableListOf<Float>()

    /** 페이드가 끝나 일시정지된 상태를 기본으로 둔다. 끊기를 보는 테스트만 true 로 바꾼다. */
    private var isMainPlaying = false

    private val mediaControllerManager: PlaybackController = mockk(relaxed = true) {
        every { isPlaying } answers { isMainPlaying }
        every { setPlaybackVolume(any()) } answers { volumes += firstArg<Float>() }
    }

    private val announcementPlayer: AudioPreviewPlayer = mockk(relaxed = true)

    private val timeSource = TestTimeSource()

    private val announcement = AudioMedia(
        id = 7,
        source = "source",
        name = "잘 자요",
        artist = "artist",
        description = "",
        audioDuration = ANNOUNCEMENT_DURATION,
        audioFileFullPath = "/audio/goodnight.m4a",
        imageFileFullPath = null,
        createdTime = LocalDateTime(2026, 11, 19, 0, 0),
    )

    @Test
    fun `페이드가 시작되기 전에는 볼륨을 건드리지 않는다`() = runTest {
        val runner = runner()

        runner.start(total = 30.minutes)
        advanceTimeBy(30.minutes - 4.seconds)
        runCurrent()

        assertEquals(emptyList<Float>(), volumes)
        verify(exactly = 0) { mediaControllerManager.pause() }
    }

    @Test
    fun `끝나기 3초 전부터 볼륨을 깎아 내리고 일시정지한다`() = runTest {
        val runner = runner()

        runner.start(total = 30.minutes)
        advanceTimeBy(30.minutes + 1.seconds)
        runCurrent()

        val ramp = volumes.dropLast(1)
        assertEquals(SleepTimerFadeOut.TICK_COUNT, ramp.size)
        ramp.zipWithNext { previous, next -> assertTrue(next < previous) }
        verify(exactly = 1) { mediaControllerManager.pause() }
    }

    @Test
    fun `깎은 볼륨은 반드시 되돌린다 — 남기면 다음 재생이 영구히 무음이 된다`() = runTest {
        val runner = runner()

        runner.start(total = 30.minutes)
        advanceTimeBy(30.minutes + 1.seconds)
        runCurrent()

        assertEquals(1f, volumes.last(), 0.0001f)
    }

    @Test
    fun `페이드 도중에 끄면 일시정지하지 않고 볼륨만 되돌린다`() = runTest {
        val runner = runner()

        runner.start(total = 30.minutes)
        advanceTimeBy(30.minutes - 2.seconds)
        runCurrent()
        runner.cancel()
        runCurrent()

        assertEquals(1f, volumes.last(), 0.0001f)
        verify(exactly = 0) { mediaControllerManager.pause() }
    }

    @Test
    fun `마무리 음성은 일시정지한 뒤에 재생한다`() = runTest {
        val runner = runner()
        runner.announcement = announcement

        runner.start(total = 30.minutes)
        advanceTimeBy(30.minutes + 1.seconds)
        runCurrent()

        verifyOrder {
            mediaControllerManager.pause()
            announcementPlayer.play("/audio/goodnight.m4a", any(), 0)
        }
    }

    @Test
    fun `마무리 음성은 길이만큼 재생하고 끝낸다`() = runTest {
        val runner = runner()
        runner.announcement = announcement

        runner.start(total = 30.minutes)
        advanceTimeBy(30.minutes + 1.seconds)
        runCurrent()
        verify(exactly = 0) { announcementPlayer.release() }

        advanceTimeBy(ANNOUNCEMENT_DURATION + 1.seconds)
        runCurrent()

        verify(exactly = 1) { announcementPlayer.release() }
    }

    @Test
    fun `마무리 음성 중에 다른 음원이 재생되면 즉시 끊는다`() = runTest {
        val runner = runner()
        runner.announcement = announcement

        runner.start(total = 30.minutes)
        advanceTimeBy(30.minutes + 1.seconds)
        runCurrent()

        isMainPlaying = true
        advanceTimeBy(1.seconds)
        runCurrent()

        verify(exactly = 1) { announcementPlayer.release() }
    }

    @Test
    fun `멈춘 뒤 마무리 음성이 나는 동안에는 타이머가 켜진 것으로 보지 않는다`() = runTest {
        val runner = runner()
        runner.announcement = announcement

        runner.start(total = 30.minutes)
        advanceTimeBy(30.minutes + 1.seconds)
        runCurrent()
        timeSource += 30.minutes

        verify(exactly = 0) { announcementPlayer.release() }
        assertNull(runner.remaining)
    }

    @Test
    fun `타이머가 도는 중에 마무리 음성을 바꾸면 바꾼 것이 난다`() = runTest {
        val runner = runner()
        runner.announcement = announcement
        runner.start(total = 30.minutes)

        val changed = announcement.copy(id = 9, name = "좋은 꿈", audioFileFullPath = "/audio/dream.m4a")
        runner.announcement = changed
        advanceTimeBy(30.minutes + 1.seconds)
        runCurrent()

        verify(exactly = 1) { announcementPlayer.play("/audio/dream.m4a", any(), 0) }
        verify(exactly = 0) { announcementPlayer.play("/audio/goodnight.m4a", any(), 0) }
    }

    @Test
    fun `마무리 음성을 고르지 않았으면 일시정지로 끝낸다`() = runTest {
        val runner = runner()

        runner.start(total = 30.minutes)
        advanceTimeBy(30.minutes + 1.seconds)
        runCurrent()

        verify(exactly = 0) { announcementPlayer.play(any(), any(), any()) }
    }

    @Test
    fun `곡이 바뀌어도 타이머는 꺼지지 않는다`() = runTest {
        val runner = runner()

        runner.start(total = 30.minutes)
        advanceTimeBy(10.minutes)
        runCurrent()
        every { mediaControllerManager.playingMediaItem } returns null
        timeSource += 10.minutes

        assertEquals(20.minutes, runner.remaining)

        advanceTimeBy(20.minutes + 1.seconds)
        runCurrent()

        verify(exactly = 1) { mediaControllerManager.pause() }
    }

    @Test
    fun `켜지 않았으면 남은 시간이 없다`() = runTest {
        assertNull(runner().remaining)
    }

    @Test
    fun `끄면 남은 시간이 사라진다`() = runTest {
        val runner = runner()

        runner.start(total = 30.minutes)
        runner.cancel()

        assertNull(runner.remaining)
    }

    private fun TestScope.runner() = SleepTimerRunner(
        playbackController = mediaControllerManager,
        announcementPlayer = announcementPlayer,
        timeSource = timeSource,
        scope = backgroundScope,
    )

    private companion object {

        val ANNOUNCEMENT_DURATION: Duration = 10.seconds
    }
}
