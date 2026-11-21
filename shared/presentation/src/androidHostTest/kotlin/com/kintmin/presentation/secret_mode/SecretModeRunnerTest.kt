package com.kintmin.presentation.secret_mode

import com.kintmin.domain.audio_track.model.SecretTrack
import com.kintmin.domain.playlist.model.Playlist
import com.kintmin.domain.playlist.model.PlaylistType
import com.kintmin.domain.playlist.usecase.FetchAllPlaylistFlowUseCase
import com.kintmin.domain.secret_mode.usecase.EndSecretModeUseCase
import com.kintmin.domain.secret_mode.usecase.FetchSecretTrackListFlowUseCase
import com.kintmin.domain.secret_mode.usecase.StartSecretModeUseCase
import com.kintmin.presentation.port.PlaybackController
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
import kotlinx.datetime.LocalDateTime
import org.junit.Assert.assertNull
import org.junit.Test
import kotlin.time.Duration
import kotlin.time.Duration.Companion.minutes
import kotlin.time.Duration.Companion.seconds

@OptIn(ExperimentalCoroutinesApi::class)
class SecretModeRunnerTest {

    private val playbackController: PlaybackController = mockk(relaxed = true)
    private val startSecretModeUseCase: StartSecretModeUseCase = mockk(relaxed = true)
    private val endSecretModeUseCase: EndSecretModeUseCase = mockk(relaxed = true)
    private val fetchAllPlaylistFlowUseCase: FetchAllPlaylistFlowUseCase = mockk {
        every { this@mockk() } returns flowOf(
            listOf(playlist(1, PlaylistType.TOTAL), playlist(4, PlaylistType.USER), playlist(5, PlaylistType.SECRET)),
        )
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
    fun `비밀 플레이리스트나 전체를 재생 중이면 비밀 곡을 큐에서 지운다`() = runTest {
        listOf(5, 1).forEach { playingId ->
            every { playbackController.currentPlaylistId } returns playingId

            runner().end()
            runCurrent()

            verify { playbackController.tryDeleteMediaItem(playingId, 10) }
            verify { playbackController.tryDeleteMediaItem(playingId, 11) }
        }
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
        fetchAllPlaylistFlowUseCase = fetchAllPlaylistFlowUseCase,
        fetchSecretTrackListFlowUseCase = fetchSecretTrackListFlowUseCase,
        scope = backgroundScope,
    )

    private fun playlist(id: Int, type: PlaylistType) = Playlist(
        id = id,
        name = "p$id",
        description = "",
        audioMediaCount = 0,
        playTimeDuration = Duration.ZERO,
        createdTime = LocalDateTime(2026, 1, 1, 0, 0),
        imageFileFullPath = null,
        isCustomImage = false,
        sequence = 0,
        type = type,
    )
}
