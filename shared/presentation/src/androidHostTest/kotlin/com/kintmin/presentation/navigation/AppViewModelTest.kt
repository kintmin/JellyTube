package com.kintmin.presentation.navigation

import com.kintmin.domain.audio_media.usecase.ImportSharedAudioMediaUseCase
import com.kintmin.domain.audio_play_setting.usecase.FetchIsPlaybackRepeatingFlowUseCase
import com.kintmin.domain.audio_play_setting.usecase.FetchIsPlaybackShufflingFlowUseCase
import com.kintmin.domain.audio_play_setting.usecase.FetchPlaybackPitchSemitoneFlowUseCase
import com.kintmin.domain.audio_play_setting.usecase.FetchPlaybackSpeedFlowUseCase
import com.kintmin.domain.secret_mode.usecase.EndSecretModeUseCase
import com.kintmin.domain.secret_mode.usecase.FetchSecretModeEnabledFlowUseCase
import com.kintmin.presentation.port.PlaybackController
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class AppViewModelTest {

    private val manager = mockk<PlaybackController>(relaxed = true)
    private val importSharedAudioMediaUseCase = mockk<ImportSharedAudioMediaUseCase>(relaxed = true)
    private val isSecretModeEnabled = MutableStateFlow(false)
    private val fetchSecretModeEnabledFlowUseCase = mockk<FetchSecretModeEnabledFlowUseCase> {
        every { this@mockk() } returns isSecretModeEnabled
    }
    private val endSecretModeUseCase = mockk<EndSecretModeUseCase>(relaxed = true)

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
            fetchSecretModeEnabledFlowUseCase = fetchSecretModeEnabledFlowUseCase,
            endSecretModeUseCase = endSecretModeUseCase,
        )

        verify { manager.setRepeatMode(true) }
        verify { manager.setShuffleMode(true) }
        verify { manager.setPlaybackSpeed(1.5f) }
        verify { manager.setPlaybackPitchSemitone(2) }
    }

    @Test
    fun `미디어 컨트롤러 초기화 intent 는 플레이어를 초기화한다`() {
        val viewModel = createViewModel()

        viewModel.sendIntent(AppIntent.InitializeMediaController)

        verify { manager.initialize() }
    }

    @Test
    fun `공유 받은 음원을 모두 가져온 뒤 재생목록 탭으로 보낸다`() = runTest {
        val viewModel = createViewModel()

        viewModel.sendIntent(AppIntent.ImportSharedAudio(listOf("a", "b")))

        coVerify { importSharedAudioMediaUseCase("a") }
        coVerify { importSharedAudioMediaUseCase("b") }
        assertEquals(NavigationEvent.NavigateToMainPlaylistsTab, viewModel.eventFlow.first())
    }

    @Test
    fun `앱이 뜰 때 비밀모드가 꺼져 있으면 남은 비밀 다운로드 대상을 정리한다`() {
        createViewModel()

        coVerify { endSecretModeUseCase() }
    }

    @Test
    fun `비밀모드가 켜진 채 화면만 다시 뜨면 끄지 않는다`() {
        isSecretModeEnabled.value = true

        createViewModel()

        coVerify(exactly = 0) { endSecretModeUseCase() }
    }

    private fun createViewModel() = AppViewModel(
        mediaControllerManager = manager,
        importSharedAudioMediaUseCase = importSharedAudioMediaUseCase,
        ensureSystemPlaylistsUseCase = mockk(relaxed = true),
        fetchIsPlaybackRepeatingFlowUseCase = mockk { every { this@mockk() } returns flowOf(false) },
        fetchIsPlaybackShufflingFlowUseCase = mockk { every { this@mockk() } returns flowOf(false) },
        fetchPlaybackSpeedFlowUseCase = mockk { every { this@mockk() } returns flowOf(1f) },
        fetchPlaybackPitchSemitoneFlowUseCase = mockk { every { this@mockk() } returns flowOf(0) },
        fetchSecretModeEnabledFlowUseCase = fetchSecretModeEnabledFlowUseCase,
        endSecretModeUseCase = endSecretModeUseCase,
    )
}
