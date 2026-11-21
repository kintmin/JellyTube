package com.kintmin.presentation.secret_mode

import com.kintmin.domain.audio_track.model.SecretTrack
import com.kintmin.domain.playlist.model.PlaylistType
import com.kintmin.domain.playlist.usecase.FetchPlaylistsByTypeUseCase
import com.kintmin.domain.secret_mode.usecase.EndSecretModeUseCase
import com.kintmin.domain.secret_mode.usecase.FetchSecretTrackListFlowUseCase
import com.kintmin.domain.secret_mode.usecase.StartSecretModeUseCase
import com.kintmin.presentation.port.PlaybackController
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
import kotlin.time.Duration.Companion.seconds

@OptIn(ExperimentalCoroutinesApi::class)
class SecretModeRunnerTest {

    private val playbackController: PlaybackController = mockk(relaxed = true)
    private val startSecretModeUseCase: StartSecretModeUseCase = mockk(relaxed = true)
    private val endSecretModeUseCase: EndSecretModeUseCase = mockk(relaxed = true)
    private val fetchPlaylistsByTypeUseCase: FetchPlaylistsByTypeUseCase = mockk {
        every { this@mockk(PlaylistType.TOTAL) } returns flowOf(listOf(testPlaylist(1, PlaylistType.TOTAL)))
        every { this@mockk(PlaylistType.SECRET) } returns flowOf(listOf(testPlaylist(5, PlaylistType.SECRET)))
    }
    private val fetchSecretTrackListFlowUseCase: FetchSecretTrackListFlowUseCase = mockk {
        every { this@mockk() } returns flowOf(listOf(SecretTrack(10, 1.seconds), SecretTrack(11, 1.seconds)))
    }

    @Test
    fun `정한 시간이 지나면 비밀모드를 끈다`() = runTest {
        val runner = runner()

        runner.start(10.minutes)
        verify { startSecretModeUseCase() }
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

        runner().end()
        runCurrent()

        verify { playbackController.tryDeleteMediaItem(1, 10) }
        verify { playbackController.tryDeleteMediaItem(1, 11) }
        verify(exactly = 0) { playbackController.clearQueue() }
    }

    @Test
    fun `일반 플레이리스트를 재생 중이면 큐를 건드리지 않는다`() = runTest {
        every { playbackController.currentPlaylistId } returns 4

        runner().end()
        runCurrent()

        coVerify { endSecretModeUseCase() }
        verify(exactly = 0) { playbackController.tryDeleteMediaItem(any(), any()) }
    }

    private fun TestScope.runner() = SecretModeRunner(
        playbackController = playbackController,
        startSecretModeUseCase = startSecretModeUseCase,
        endSecretModeUseCase = endSecretModeUseCase,
        fetchPlaylistsByTypeUseCase = fetchPlaylistsByTypeUseCase,
        fetchSecretTrackListFlowUseCase = fetchSecretTrackListFlowUseCase,
        scope = backgroundScope,
    )

}
