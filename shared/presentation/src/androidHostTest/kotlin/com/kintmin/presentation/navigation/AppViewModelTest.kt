package com.kintmin.presentation.navigation

import com.kintmin.domain.audio_media.usecase.ImportSharedAudioMediaUseCase
import com.kintmin.domain.audio_play_setting.usecase.FetchIsPlaybackRepeatingFlowUseCase
import com.kintmin.domain.audio_play_setting.usecase.FetchIsPlaybackShufflingFlowUseCase
import com.kintmin.domain.audio_play_setting.usecase.FetchPlaybackPitchSemitoneFlowUseCase
import com.kintmin.domain.audio_play_setting.usecase.FetchPlaybackSpeedFlowUseCase
import com.kintmin.domain.backup.usecase.BackupIfDueUseCase
import com.kintmin.domain.playlist.model.PlaylistType
import com.kintmin.domain.playlist.usecase.FetchPlaylistsByTypeUseCase
import com.kintmin.domain.secret_mode.usecase.EndSecretModeUseCase
import com.kintmin.domain.secret_mode.usecase.FetchSecretAudioMediaIdListFlowUseCase
import com.kintmin.domain.secret_mode.usecase.FetchSecretModeEnabledFlowUseCase
import com.kintmin.log.AppLog
import com.kintmin.presentation.deeplink.DeepLinkConstants
import com.kintmin.presentation.port.PlaybackController
import com.kintmin.presentation.testPlaylist
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
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
    private val fetchPlaylistsByTypeUseCase = mockk<FetchPlaylistsByTypeUseCase> {
        every { this@mockk(PlaylistType.SECRET) } returns flowOf(listOf(testPlaylist(SECRET_PLAYLIST_ID, PlaylistType.SECRET)))
        every { this@mockk(PlaylistType.SECRET_FAVORITE) } returns
            flowOf(listOf(testPlaylist(SECRET_FAVORITE_PLAYLIST_ID, PlaylistType.SECRET_FAVORITE)))
    }
    private val fetchSecretAudioMediaIdListFlowUseCase = mockk<FetchSecretAudioMediaIdListFlowUseCase> {
        every { this@mockk() } returns flowOf(listOf(SECRET_AUDIO_MEDIA_ID))
    }
    private val backupIfDueUseCase = mockk<BackupIfDueUseCase> {
        coEvery { this@mockk(any(), any()) } returns Result.success(null)
    }
    private val appLog = mockk<AppLog>(relaxed = true)

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
            fetchPlaylistsByTypeUseCase = fetchPlaylistsByTypeUseCase,
            fetchSecretAudioMediaIdListFlowUseCase = fetchSecretAudioMediaIdListFlowUseCase,
            backupIfDueUseCase = mockk(),
            appLog = appLog,
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
        assertEquals(NavigationEvent.NavigateToMainPlaylistsTab, viewModel.eventFlow.first { it !is NavigationEvent.LeaveSecretScreens })
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

    @Test
    fun `비밀모드가 끝나면 비밀 화면을 닫게 한다`() = runTest {
        isSecretModeEnabled.value = true
        val viewModel = createViewModel()

        isSecretModeEnabled.value = false

        assertEquals(NavigationEvent.LeaveSecretScreens, viewModel.eventFlow.first())
    }

    @Test
    fun `앱이 뜰 때 비밀모드가 꺼져 있으면 복원된 비밀 화면을 닫게 한다`() = runTest {
        val viewModel = createViewModel()

        assertEquals(NavigationEvent.LeaveSecretScreens, viewModel.eventFlow.first())
    }

    @Test
    fun `비밀모드가 꺼져 있으면 비밀 플레이리스트 안의 곡 상세 딥링크도 따르지 않는다`() = runTest {
        val viewModel = createViewModel()
        val events = mutableListOf<NavigationEvent>()
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { viewModel.eventFlow.collect { events += it } }

        viewModel.sendIntent(
            AppIntent.HandleDeepLink(DeepLinkConstants.UrlBuilder.playlistAudioMediaDetailScreen(SECRET_PLAYLIST_ID, 9)),
        )

        assertFalse(events.any { it is NavigationEvent.NavigateToAudioMedia })
    }

    @Test
    fun `비밀모드가 꺼져 있으면 비밀 곡의 곡 상세·가사 딥링크를 따르지 않는다`() = runTest {
        val viewModel = createViewModel()
        val events = mutableListOf<NavigationEvent>()
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { viewModel.eventFlow.collect { events += it } }

        viewModel.sendIntent(AppIntent.HandleDeepLink(DeepLinkConstants.UrlBuilder.audioMediaDetailScreen(SECRET_AUDIO_MEDIA_ID)))
        viewModel.sendIntent(AppIntent.HandleDeepLink(DeepLinkConstants.UrlBuilder.lyricsViewerScreen(SECRET_AUDIO_MEDIA_ID)))
        viewModel.sendIntent(AppIntent.HandleDeepLink(DeepLinkConstants.UrlBuilder.audioMediaDetailScreen(3)))

        assertFalse(events.any { it is NavigationEvent.NavigateToAudioMedia && it.audioMediaId == SECRET_AUDIO_MEDIA_ID })
        assertFalse(events.any { it is NavigationEvent.NavigateToLyricsViewer })
        assertEquals(true, events.any { it is NavigationEvent.NavigateToAudioMedia && it.audioMediaId == 3 })
    }

    @Test
    fun `비밀모드가 꺼져 있으면 비밀 플레이리스트로 가는 딥링크를 따르지 않는다`() = runTest {
        val viewModel = createViewModel()
        val events = mutableListOf<NavigationEvent>()
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { viewModel.eventFlow.collect { events += it } }

        viewModel.sendIntent(AppIntent.HandleDeepLink(playlistUrl(SECRET_PLAYLIST_ID)))
        viewModel.sendIntent(AppIntent.HandleDeepLink(playlistUrl(4)))

        assertFalse(events.any { it is NavigationEvent.NavigateToPlaylistContent && it.playlistId == SECRET_PLAYLIST_ID })
        assertEquals(true, events.any { it is NavigationEvent.NavigateToPlaylistContent && it.playlistId == 4 })
    }

    @Test
    fun `우리 주소가 아니면 이동하지 않고 그 주소를 로그에 남긴다`() = runTest {
        val viewModel = createViewModel()

        viewModel.sendIntent(AppIntent.HandleDeepLink("https://example.com"))

        verify { appLog.sendDebugLog(match { it.tag == "DeepLink" && it.message.endsWith("https://example.com") }) }
    }

    @Test
    fun `비밀 화면이라 버린 딥링크도 로그에 남긴다`() = runTest {
        val viewModel = createViewModel()

        viewModel.sendIntent(AppIntent.HandleDeepLink(playlistUrl(SECRET_PLAYLIST_ID)))

        verify { appLog.sendDebugLog(match { it.message.startsWith("비밀 화면이라") }) }
    }

    private fun playlistUrl(playlistId: Int) = DeepLinkConstants.UrlBuilder.playlistContentScreen(playlistId)

    @Test
    fun `앱이 내려가면 오늘 백업할 때인지 확인한다`() = runTest {
        val viewModel = createViewModel()

        viewModel.sendIntent(AppIntent.OnAppBackground)

        coVerify { backupIfDueUseCase(any(), any()) }
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
        fetchPlaylistsByTypeUseCase = fetchPlaylistsByTypeUseCase,
        fetchSecretAudioMediaIdListFlowUseCase = fetchSecretAudioMediaIdListFlowUseCase,
        backupIfDueUseCase = backupIfDueUseCase,
        appLog = appLog,
    )


    private companion object {
        const val SECRET_PLAYLIST_ID = 5
        const val SECRET_FAVORITE_PLAYLIST_ID = 6
        const val SECRET_AUDIO_MEDIA_ID = 9
    }
}
