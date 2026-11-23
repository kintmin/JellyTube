package com.kintmin.presentation.secret_mode

import com.kintmin.domain.playlist.model.PlaylistType
import com.kintmin.domain.playlist.usecase.FetchPlaylistsByTypeUseCase
import com.kintmin.domain.secret_mode.usecase.EndSecretModeUseCase
import com.kintmin.domain.secret_mode.usecase.FetchSecretAudioMediaIdListFlowUseCase
import com.kintmin.domain.secret_mode.usecase.StartSecretModeUseCase
import com.kintmin.presentation.port.PlaybackController
import com.kintmin.presentation.port.model.MediaControlData
import com.kintmin.presentation.testPlaylist
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertNull
import org.junit.Test
import kotlin.time.Duration.Companion.minutes
import kotlin.time.Clock
import kotlin.time.Duration.Companion.seconds
import kotlin.time.Instant

@OptIn(ExperimentalCoroutinesApi::class)
class SecretModeRunnerTest {

    private val playbackController: PlaybackController = mockk(relaxed = true)
    private val startSecretModeUseCase: StartSecretModeUseCase = mockk(relaxed = true)
    private val endSecretModeUseCase: EndSecretModeUseCase = mockk(relaxed = true)
    private val fetchPlaylistsByTypeUseCase: FetchPlaylistsByTypeUseCase = mockk {
        every { this@mockk(PlaylistType.TOTAL) } returns flowOf(listOf(testPlaylist(1, PlaylistType.TOTAL)))
        every { this@mockk(PlaylistType.SECRET) } returns flowOf(listOf(testPlaylist(5, PlaylistType.SECRET)))
        every { this@mockk(PlaylistType.SECRET_FAVORITE) } returns
            flowOf(listOf(testPlaylist(6, PlaylistType.SECRET_FAVORITE)))
    }
    private val fetchSecretAudioMediaIdListFlowUseCase: FetchSecretAudioMediaIdListFlowUseCase = mockk {
        every { this@mockk() } returns flowOf(listOf(10, 11))
    }

    @Test
    fun `정한 시간이 지나면 비밀모드를 끈다`() = runTest {
        val runner = runner()

        runner.start(10.minutes)
        coVerify { startSecretModeUseCase() }
        advanceTimeBy(10.minutes - 1.seconds)
        coVerify(exactly = 0) { endSecretModeUseCase() }

        advanceTimeBy(2.seconds)
        coVerify { endSecretModeUseCase() }
        assertNull(runner.remaining)
    }

    @Test
    fun `비밀 플레이리스트를 재생 중이면 큐를 통째로 비운다`() = runTest {
        every { playbackController.currentPlaylistId } returns 5

        runner().end()
        runCurrent()

        verify { playbackController.clearQueue() }
        verify(exactly = 0) { playbackController.tryDeleteMediaItem(any(), any()) }
    }

    @Test
    fun `전체를 재생 중이면 비밀 곡만 큐에서 지운다`() = runTest {
        every { playbackController.currentPlaylistId } returns 1
        every { playbackController.playingMediaItem } returns playing(3)

        runner().end()
        runCurrent()

        verify { playbackController.tryDeleteMediaItem(1, 10) }
        verify { playbackController.tryDeleteMediaItem(1, 11) }
        verify(exactly = 0) { playbackController.clearQueue() }
    }

    @Test
    fun `전체에서 듣던 곡이 비밀 곡이면 다음 곡으로 넘어가지 않게 큐를 비운다`() = runTest {
        every { playbackController.currentPlaylistId } returns 1
        every { playbackController.playingMediaItem } returns playing(10)

        runner().end()
        runCurrent()

        verify { playbackController.clearQueue() }
        verify(exactly = 0) { playbackController.tryDeleteMediaItem(any(), any()) }
    }

    @Test
    fun `기기가 잠든 사이 종료 시각이 지났으면 앱이 다시 보일 때 끈다`() = runTest {
        val clock = MutableClock()
        val runner = runner(clock)
        runner.start(10.minutes)

        clock.now += 9.minutes
        runner.endIfExpired()
        coVerify(exactly = 0) { endSecretModeUseCase() }

        clock.now += 2.minutes
        runner.endIfExpired()
        coVerify { endSecretModeUseCase() }
        assertNull(runner.remaining)
    }

    @Test
    fun `일반 플레이리스트를 재생 중이면 큐를 건드리지 않는다`() = runTest {
        every { playbackController.currentPlaylistId } returns 4

        runner().end()
        runCurrent()

        coVerify { endSecretModeUseCase() }
        verify(exactly = 0) { playbackController.tryDeleteMediaItem(any(), any()) }
    }

    private class MutableClock : Clock {
        var now: Instant = Instant.fromEpochMilliseconds(0)
        override fun now() = now
    }

    private fun playing(mediaId: Int): MediaControlData = mockk { every { this@mockk.mediaId } returns mediaId.toString() }

    private fun TestScope.runner(clock: Clock = Clock.System) = SecretModeRunner(
        playbackController = playbackController,
        startSecretModeUseCase = startSecretModeUseCase,
        endSecretModeUseCase = endSecretModeUseCase,
        fetchPlaylistsByTypeUseCase = fetchPlaylistsByTypeUseCase,
        fetchSecretAudioMediaIdListFlowUseCase = fetchSecretAudioMediaIdListFlowUseCase,
        clock = clock,
        scope = backgroundScope,
    )

}
