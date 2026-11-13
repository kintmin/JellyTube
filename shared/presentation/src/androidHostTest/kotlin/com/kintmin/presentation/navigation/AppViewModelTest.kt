package com.kintmin.presentation.navigation

import com.kintmin.domain.audio_play_setting.usecase.FetchIsPlaybackRepeatingFlowUseCase
import com.kintmin.domain.audio_play_setting.usecase.FetchIsPlaybackShufflingFlowUseCase
import com.kintmin.domain.audio_play_setting.usecase.FetchPlaybackPitchSemitoneFlowUseCase
import com.kintmin.domain.audio_play_setting.usecase.FetchPlaybackSpeedFlowUseCase
import com.kintmin.presentation.port.MediaControllerManager
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class AppViewModelTest {

    private val manager = mockk<MediaControllerManager>(relaxed = true)

    @Before
    fun setUp() {
        Dispatchers.setMain(UnconfinedTestDispatcher())
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `저장한 재생 설정은 화면을 열지 않아도 앱이 뜨면 플레이어에 건다`() {
        AppViewModel(
            mediaControllerManager = manager,
            importSharedAudioMediaUseCase = mockk(),
            ensureSystemPlaylistsUseCase = mockk(relaxed = true),
            fetchIsPlaybackRepeatingFlowUseCase = mockk { every { this@mockk() } returns flowOf(true) },
            fetchIsPlaybackShufflingFlowUseCase = mockk { every { this@mockk() } returns flowOf(true) },
            fetchPlaybackSpeedFlowUseCase = mockk { every { this@mockk() } returns flowOf(1.5f) },
            fetchPlaybackPitchSemitoneFlowUseCase = mockk { every { this@mockk() } returns flowOf(2) },
        )

        verify { manager.setRepeatMode(true) }
        verify { manager.setShuffleMode(true) }
        verify { manager.setPlaybackSpeed(1.5f) }
        verify { manager.setPlaybackPitchSemitone(2) }
    }
}
