package com.kintmin.presentation.ui.player_detail

import com.kintmin.domain.audio_play_setting.model.RepeatRangeState
import com.kintmin.presentation.port.PlaybackController
import com.kintmin.presentation.sleep_timer.SleepTimerRunner
import com.kintmin.presentation.port.model.MediaControlData
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import kotlin.time.Duration.Companion.minutes

/** 잠들기 타이머가 곡 교체에 휩쓸리지 않는지 본다 — `refreshMediaData` 가 UiState 를 새로 만든다. */
@OptIn(ExperimentalCoroutinesApi::class)
class PlayerDetailViewModelTest {

    private fun mediaControlData(mediaId: String) = MediaControlData(
        mediaId = mediaId,
        mediaFileUri = "/audios/$mediaId.m4a",
        mediaTitle = "제목$mediaId",
        mediaDescription = "",
        mediaArtist = "",
        mediaDurationMs = 51_000L,
        mediaArtworkFileUri = null,
    )

    private var playing: MediaControlData? = mediaControlData("1")

    private val manager = mockk<PlaybackController>(relaxed = true) {
        every { playingMediaItem } answers { playing }
        every { currentPosition } returns 3_000L
        every { currentPlaylistId } returns 1
        every { repeatRangeState } returns RepeatRangeState()
    }

    private val sleepTimerRunner = mockk<SleepTimerRunner>(relaxed = true) {
        every { remaining } returns 10.minutes
    }

    @Before
    fun setUp() {
        Dispatchers.setMain(UnconfinedTestDispatcher())
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `타이머 창이 떠 있는 동안 곡이 바뀌어도 창이 닫히지 않는다`() {
        val viewModel = viewModel()
        viewModel.sendIntent(PlayerDetailIntent.OnClickSleepTimerButton)
        assertTrue(viewModel.data.value.isSleepTimerMenuVisible)

        playing = mediaControlData("2")
        viewModel.sendIntent(PlayerDetailIntent.OnRefreshMediaData)

        assertTrue(viewModel.data.value.isSleepTimerMenuVisible)
    }

    @Test
    fun `마무리 음성 창이 떠 있는 동안 곡이 바뀌어도 창이 닫히지 않는다`() {
        val viewModel = viewModel()
        viewModel.sendIntent(PlayerDetailIntent.OnClickSleepTimerAnnouncementButton)

        playing = mediaControlData("2")
        viewModel.sendIntent(PlayerDetailIntent.OnRefreshMediaData)

        assertTrue(viewModel.data.value.isSleepTimerAnnouncementMenuVisible)
    }

    @Test
    fun `곡이 바뀌어도 남은 시간은 이어진다`() {
        val viewModel = viewModel()

        playing = mediaControlData("2")
        viewModel.sendIntent(PlayerDetailIntent.OnRefreshMediaData)

        assertEquals(10.minutes, viewModel.data.value.sleepTimerRemaining)
    }

    @Test
    fun `타이머를 켜면 정한 분으로 시작하고 창을 닫는다`() {
        val viewModel = viewModel()
        viewModel.sendIntent(PlayerDetailIntent.OnClickSleepTimerButton)

        viewModel.sendIntent(PlayerDetailIntent.OnStartSleepTimer)

        verify(exactly = 1) { sleepTimerRunner.start(total = 30.minutes) }
        assertFalse(viewModel.data.value.isSleepTimerMenuVisible)
    }

    @Test
    fun `타이머를 끄면 창을 닫고 남은 시간을 다시 읽는다`() {
        val viewModel = viewModel()
        viewModel.sendIntent(PlayerDetailIntent.OnClickSleepTimerButton)
        every { sleepTimerRunner.remaining } returns null

        viewModel.sendIntent(PlayerDetailIntent.OnCancelSleepTimer)

        verify(exactly = 1) { sleepTimerRunner.cancel() }
        assertFalse(viewModel.data.value.isSleepTimerMenuVisible)
        assertEquals(null, viewModel.data.value.sleepTimerRemaining)
    }

    private fun viewModel() = PlayerDetailViewModel(
        mediaControllerManager = manager,
        sleepTimerRunner = sleepTimerRunner,
        fetchIsPlaybackRepeatingFlowUseCase = mockk(relaxed = true),
        fetchIsPlaybackShufflingFlowUseCase = mockk(relaxed = true),
        fetchPlaybackSpeedFlowUseCase = mockk(relaxed = true),
        fetchPlaybackPitchSemitoneFlowUseCase = mockk(relaxed = true),
        updatePlaybackRepeatingUseCase = mockk(relaxed = true),
        updateIsPlaybackShufflingUseCase = mockk(relaxed = true),
        updatePlaybackSpeedUseCase = mockk(relaxed = true),
        updatePlaybackPitchSemitoneUseCase = mockk(relaxed = true),
        updateSleepTimerMinutesUseCase = mockk(relaxed = true),
        updateSleepTimerAnnouncementMediaIdUseCase = mockk(relaxed = true),
        fetchSleepTimerMinutesFlowUseCase = mockk(relaxed = true),
        fetchSleepTimerAnnouncementOptionsFlowUseCase = mockk(relaxed = true),
        fetchPlaylistFlowUseCase = mockk(relaxed = true),
        fetchAudioMediaDetailFlowUseCase = mockk(relaxed = true),
        toggleFavoriteUseCase = mockk(relaxed = true),
        getParsedLyricsUseCase = mockk(relaxed = true),
    )
}
