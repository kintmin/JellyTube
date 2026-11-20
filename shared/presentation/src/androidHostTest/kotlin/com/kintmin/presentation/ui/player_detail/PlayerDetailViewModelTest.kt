package com.kintmin.presentation.ui.player_detail

import com.kintmin.domain.audio_play_setting.model.RepeatRangeState
import com.kintmin.domain.audio_track.model.PlaylistTrackAggregate
import com.kintmin.domain.audio_track.usecase.FetchAudioMediaDetailFlowUseCase
import com.kintmin.domain.lyrics.model.LyricsLine
import com.kintmin.domain.lyrics.usecase.GetParsedLyricsUseCase
import com.kintmin.domain.playlist.model.PlaylistType
import com.kintmin.presentation.port.PlaybackController
import com.kintmin.presentation.sleep_timer.SleepTimerRunner
import com.kintmin.presentation.port.model.MediaControlData
import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import kotlin.time.Duration.Companion.minutes

/** 잠들기 타이머가 곡 교체에 휩쓸리지 않는지, 그리고 가사 세 상태의 분기를 본다. */
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

    /** 곡에 딸린 가사 파일 경로. null 이면 가사 없는 곡이다. */
    private var lyricFilePath: String? = null

    /** 그 파일을 파싱한 결과. timeMs 가 없으면 비싱크 가사다. */
    private var parsedLines: List<LyricsLine> = emptyList()

    private val manager = mockk<PlaybackController>(relaxed = true) {
        every { playingMediaItem } answers { playing }
        every { currentPosition } returns 3_000L
        every { currentPlaylistId } returns 1
        every { repeatRangeState } returns RepeatRangeState()
    }

    private val sleepTimerRunner = mockk<SleepTimerRunner>(relaxed = true) {
        every { remaining } returns 10.minutes
    }

    // ViewModel 이 읽는 것은 플레이리스트 종류와 가사 파일 경로 둘뿐이라 그 둘만 세운다.
    private fun aggregate() = mockk<PlaylistTrackAggregate>(relaxed = true) {
        every { playlist.type } returns PlaylistType.USER
        every { audioMedia.lyricFileFullPath } returns lyricFilePath
    }

    private val fetchAudioMediaDetail = mockk<FetchAudioMediaDetailFlowUseCase> {
        every { this@mockk.invoke(any()) } answers { flowOf(listOf(aggregate())) }
    }

    private val getParsedLyrics = mockk<GetParsedLyricsUseCase> {
        coEvery { this@mockk.invoke(any()) } answers { parsedLines }
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

    // ---- 가사 세 상태 ----

    @Test
    fun `싱크 가사면 재생 위치에 맞는 줄이 활성이다`() {
        givenSyncedLyrics()
        val viewModel = viewModel()

        // currentPosition 은 3초. 2초 줄이 활성이다.
        assertTrue(viewModel.data.value.hasLyrics)
        assertEquals(1, viewModel.data.value.activeLyricIndex)
        assertEquals("두 번째 줄", viewModel.data.value.currentLyricLine)
    }

    @Test
    fun `비싱크 가사면 활성 줄이 없지만 가사는 있다`() {
        givenUnsyncedLyrics()
        val viewModel = viewModel()

        assertTrue(viewModel.data.value.hasLyrics)
        assertEquals(-1, viewModel.data.value.activeLyricIndex)
        assertEquals("", viewModel.data.value.currentLyricLine)
        assertEquals(2, viewModel.data.value.lyricLines.size)
    }

    @Test
    fun `가사가 있으면 가사 입구를 눌러 패널을 열고 닫는다`() {
        givenSyncedLyrics()
        val viewModel = viewModel()

        viewModel.sendIntent(PlayerDetailIntent.OnClickLyrics)
        assertTrue(viewModel.data.value.showsLyricsPanel)

        viewModel.sendIntent(PlayerDetailIntent.OnClickLyrics)
        assertFalse(viewModel.data.value.showsLyricsPanel)
    }

    @Test
    fun `패널을 연 채 가사 없는 곡으로 넘어가면 아트워크로 돌아간다`() {
        givenSyncedLyrics()
        val viewModel = viewModel()
        viewModel.sendIntent(PlayerDetailIntent.OnClickLyrics)
        assertTrue(viewModel.data.value.showsLyricsPanel)

        lyricFilePath = null
        parsedLines = emptyList()
        playing = mediaControlData("2")
        viewModel.sendIntent(PlayerDetailIntent.OnRefreshMediaData)

        assertFalse(viewModel.data.value.hasLyrics)
        assertFalse(viewModel.data.value.showsLyricsPanel)
        // 사용자의 뜻은 지우지 않는다 — 가사 있는 곡이 오면 패널이 다시 뜬다.
        assertTrue(viewModel.data.value.isLyricsPanelVisible)
    }

    @Test
    fun `비싱크 가사도 패널은 열린다`() {
        givenUnsyncedLyrics()
        val viewModel = viewModel()

        viewModel.sendIntent(PlayerDetailIntent.OnClickLyrics)

        assertTrue(viewModel.data.value.showsLyricsPanel)
    }

    @Test
    fun `가사가 없으면 가사 입구가 가사 찾기로 보낸다`() = runTest {
        val viewModel = viewModel()
        val events = mutableListOf<PlayerDetailEvent>()
        val collector = launch(UnconfinedTestDispatcher(testScheduler)) {
            viewModel.eventFlow.collect { events += it }
        }

        viewModel.sendIntent(PlayerDetailIntent.OnClickLyrics)

        assertEquals(
            listOf(
                PlayerDetailEvent.NavigateToLyricsSearch(
                    audioMediaId = 1,
                    query = "제목1",
                    durationSeconds = 51.0,
                )
            ),
            events,
        )
        assertFalse(viewModel.data.value.showsLyricsPanel)
        collector.cancel()
    }

    private fun givenSyncedLyrics() {
        lyricFilePath = "/lyrics/1.lrc"
        parsedLines = listOf(
            LyricsLine(timeMs = 0L, text = "첫 줄"),
            LyricsLine(timeMs = 2_000L, text = "두 번째 줄"),
            LyricsLine(timeMs = 5_000L, text = "세 번째 줄"),
        )
    }

    private fun givenUnsyncedLyrics() {
        lyricFilePath = "/lyrics/1.txt"
        parsedLines = listOf(
            LyricsLine(timeMs = null, text = "첫 줄"),
            LyricsLine(timeMs = null, text = "두 번째 줄"),
        )
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
        fetchAudioMediaDetailFlowUseCase = fetchAudioMediaDetail,
        toggleFavoriteUseCase = mockk(relaxed = true),
        getParsedLyricsUseCase = getParsedLyrics,
    )
}
