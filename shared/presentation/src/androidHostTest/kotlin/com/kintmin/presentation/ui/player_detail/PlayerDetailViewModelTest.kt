package com.kintmin.presentation.ui.player_detail

import com.kintmin.domain.audio_play_setting.model.RepeatRangeState
import com.kintmin.domain.audio_track.model.PlaylistTrackAggregate
import com.kintmin.domain.audio_track.usecase.FetchAudioMediaDetailFlowUseCase
import com.kintmin.domain.lyrics.model.LyricsLine
import com.kintmin.domain.lyrics.model.LyricsVariant
import com.kintmin.domain.lyrics.usecase.GetLyricsVariantUseCase
import com.kintmin.domain.lyrics.usecase.GetParsedLyricsUseCase
import com.kintmin.domain.playlist.model.PlaylistType
import com.kintmin.presentation.port.PlaybackController
import com.kintmin.presentation.sleep_timer.SleepTimerRunner
import com.kintmin.presentation.port.model.MediaControlData
import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk
import io.mockk.slot
import io.mockk.verify
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import kotlin.time.Duration
import kotlin.time.Duration.Companion.milliseconds
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

    // ViewModel 이 읽는 것만 세운다. 기본 이름은 재생기가 주는 값과 같게 둬서,
    // 저장소에서 다시 덮어도 다른 테스트의 기대가 흔들리지 않게 한다.
    private fun aggregate(
        name: String = "제목1",
        artist: String = "",
    ) = mockk<PlaylistTrackAggregate>(relaxed = true) {
        every { playlist.type } returns PlaylistType.USER
        every { audioMedia.lyricFileFullPath } returns lyricFilePath
        every { audioMedia.name } returns name
        every { audioMedia.artist } returns artist
    }

    private val fetchAudioMediaDetail = mockk<FetchAudioMediaDetailFlowUseCase> {
        every { this@mockk.invoke(any()) } answers { flowOf(listOf(aggregate())) }
    }

    private val getParsedLyrics = mockk<GetParsedLyricsUseCase> {
        coEvery { this@mockk.invoke(any()) } answers { parsedLines }
    }

    /** 음차·번역 파일을 파싱한 결과. 없는 variant 는 파일이 없는 것이다. */
    private var variantLines: Map<LyricsVariant, List<LyricsLine>> = emptyMap()

    private val getLyricsVariant = mockk<GetLyricsVariantUseCase> {
        coEvery { this@mockk.invoke(any(), any()) } answers { Result.success(variantLines[secondArg()]) }
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
        assertTrue(viewModel.uiState.value.isSleepTimerMenuVisible)

        playing = mediaControlData("2")
        viewModel.sendIntent(PlayerDetailIntent.OnRefreshMediaData)

        assertTrue(viewModel.uiState.value.isSleepTimerMenuVisible)
    }

    @Test
    fun `마무리 음원 창이 떠 있는 동안 곡이 바뀌어도 창이 닫히지 않는다`() {
        val viewModel = viewModel()
        viewModel.sendIntent(PlayerDetailIntent.OnClickSleepTimerAnnouncementButton)

        playing = mediaControlData("2")
        viewModel.sendIntent(PlayerDetailIntent.OnRefreshMediaData)

        assertTrue(viewModel.uiState.value.isSleepTimerAnnouncementMenuVisible)
    }

    @Test
    fun `곡이 바뀌어도 남은 시간은 이어진다`() {
        val viewModel = viewModel()

        playing = mediaControlData("2")
        viewModel.sendIntent(PlayerDetailIntent.OnRefreshMediaData)

        assertEquals(10.minutes, viewModel.uiState.value.sleepTimerRemaining)
    }

    @Test
    fun `타이머를 켜면 정한 분으로 시작하고 창을 닫는다`() {
        val viewModel = viewModel()
        viewModel.sendIntent(PlayerDetailIntent.OnClickSleepTimerButton)

        viewModel.sendIntent(PlayerDetailIntent.OnStartSleepTimer)

        verify(exactly = 1) { sleepTimerRunner.start(total = 30.minutes) }
        assertFalse(viewModel.uiState.value.isSleepTimerMenuVisible)
    }

    @Test
    fun `타이머를 끄면 창을 닫고 남은 시간을 다시 읽는다`() {
        val viewModel = viewModel()
        viewModel.sendIntent(PlayerDetailIntent.OnClickSleepTimerButton)
        every { sleepTimerRunner.remaining } returns null

        viewModel.sendIntent(PlayerDetailIntent.OnCancelSleepTimer)

        verify(exactly = 1) { sleepTimerRunner.cancel() }
        assertFalse(viewModel.uiState.value.isSleepTimerMenuVisible)
        assertEquals(null, viewModel.uiState.value.sleepTimerRemaining)
    }

    // ---- 가사 세 상태 ----

    @Test
    fun `싱크 가사면 재생 위치에 맞는 줄이 활성이다`() {
        givenSyncedLyrics()
        val viewModel = viewModel()

        // currentPosition 은 3초. 2초 줄이 활성이다.
        assertTrue(viewModel.uiState.value.hasLyrics)
        assertEquals(1, viewModel.uiState.value.activeLyricIndex)
        assertEquals("두 번째 줄", viewModel.uiState.value.currentLyricLine)
    }

    @Test
    fun `비싱크 가사면 활성 줄이 없지만 가사는 있다`() {
        givenUnsyncedLyrics()
        val viewModel = viewModel()

        assertTrue(viewModel.uiState.value.hasLyrics)
        assertEquals(-1, viewModel.uiState.value.activeLyricIndex)
        assertEquals("", viewModel.uiState.value.currentLyricLine)
        assertEquals(2, viewModel.uiState.value.lyricLines.size)
    }

    @Test
    fun `다음 줄이 폴링 사이에 오면 그 시각에 활성 줄이 넘어간다`() = runTest {
        givenSyncedLyrics()
        every { manager.currentPosition } returns 4_900L
        every { manager.isPlaying } returns true
        every { manager.playbackSpeed } returns 1f
        val viewModel = viewModel()
        assertEquals(1, viewModel.uiState.value.activeLyricIndex)

        advanceTimeBy(100)
        runCurrent()

        assertEquals(2, viewModel.uiState.value.activeLyricIndex)
    }

    @Test
    fun `멈춘 동안에는 다음 줄로 넘어가지 않는다`() = runTest {
        givenSyncedLyrics()
        every { manager.currentPosition } returns 4_900L
        every { manager.isPlaying } returns true
        every { manager.playbackSpeed } returns 1f
        val viewModel = viewModel()

        every { manager.isPlaying } returns false
        advanceTimeBy(100)
        runCurrent()

        assertEquals(1, viewModel.uiState.value.activeLyricIndex)
    }

    @Test
    fun `줄을 눌러 옮기면 옮기기 전에 잡힌 다음 줄로 넘어가지 않는다`() = runTest {
        givenSyncedLyrics()
        var position = 4_900L
        every { manager.currentPosition } answers { position }
        val seekTo = slot<Duration>()
        every { manager.seek(capture(seekTo)) } answers { position = seekTo.captured.inWholeMilliseconds }
        every { manager.isPlaying } returns true
        every { manager.playbackSpeed } returns 1f
        val viewModel = viewModel()

        viewModel.sendIntent(PlayerDetailIntent.OnClickLyricLine(0))
        advanceTimeBy(100)
        runCurrent()

        assertEquals(0, viewModel.uiState.value.activeLyricIndex)
    }

    @Test
    fun `음차·번역 파일이 있으면 같은 번호의 줄에 붙는다`() {
        givenSyncedLyrics()
        variantLines = mapOf(
            LyricsVariant.TRANSLATION to listOf(
                LyricsLine(timeMs = 0L, text = "first"),
                LyricsLine(timeMs = 2_000L, text = "second"),
                LyricsLine(timeMs = 5_000L, text = ""),
            ),
        )
        val viewModel = viewModel()

        val lines = viewModel.uiState.value.lyricLines
        assertEquals(listOf("first", "second", null), lines.map { it.translation })
        assertEquals(listOf(null, null, null), lines.map { it.transliteration })
        assertEquals("second", viewModel.uiState.value.currentLyricTranslation)
    }

    @Test
    fun `음차·번역 파일이 없으면 원문만 있다`() {
        givenSyncedLyrics()
        val viewModel = viewModel()

        val lines = viewModel.uiState.value.lyricLines
        assertTrue(lines.all { it.translation == null && it.transliteration == null })
        assertEquals(null, viewModel.uiState.value.currentLyricTranslation)
    }

    @Test
    fun `싱크 가사의 줄을 누르면 그 줄의 시각으로 옮긴다`() {
        givenSyncedLyrics()
        val viewModel = viewModel()

        viewModel.sendIntent(PlayerDetailIntent.OnClickLyricLine(2))

        assertTrue(viewModel.uiState.value.isLyricsSynced)
        verify(exactly = 1) { manager.seek(5_000.milliseconds) }
    }

    @Test
    fun `비싱크 가사의 줄을 눌러도 옮기지 않는다`() {
        givenUnsyncedLyrics()
        val viewModel = viewModel()

        viewModel.sendIntent(PlayerDetailIntent.OnClickLyricLine(1))

        assertFalse(viewModel.uiState.value.isLyricsSynced)
        verify(exactly = 0) { manager.seek(any()) }
    }

    @Test
    fun `가사가 있으면 가사 입구를 눌러 패널을 열고 닫는다`() {
        givenSyncedLyrics()
        val viewModel = viewModel()

        viewModel.sendIntent(PlayerDetailIntent.OnClickLyrics)
        assertTrue(viewModel.uiState.value.showsLyricsFace)

        viewModel.sendIntent(PlayerDetailIntent.OnClickLyrics)
        assertFalse(viewModel.uiState.value.showsLyricsFace)
    }

    @Test
    fun `패널을 연 채 가사 없는 곡으로 넘어가면 아트워크로 돌아간다`() {
        givenSyncedLyrics()
        val viewModel = viewModel()
        viewModel.sendIntent(PlayerDetailIntent.OnClickLyrics)
        assertTrue(viewModel.uiState.value.showsLyricsFace)

        lyricFilePath = null
        parsedLines = emptyList()
        playing = mediaControlData("2")
        viewModel.sendIntent(PlayerDetailIntent.OnRefreshMediaData)

        assertFalse(viewModel.uiState.value.hasLyrics)
        assertFalse(viewModel.uiState.value.showsLyricsFace)
        // 사용자의 뜻은 지우지 않는다 — 가사 있는 곡이 오면 패널이 다시 뜬다.
        assertTrue(viewModel.uiState.value.isLyricsPanelVisible)
    }

    @Test
    fun `비싱크 가사도 패널은 열린다`() {
        givenUnsyncedLyrics()
        val viewModel = viewModel()

        viewModel.sendIntent(PlayerDetailIntent.OnClickLyrics)

        assertTrue(viewModel.uiState.value.showsLyricsFace)
    }

    @Test
    fun `가사가 없으면 가사 입구가 그 자리에서 없다고 알린다`() = runTest {
        val viewModel = viewModel()
        val events = mutableListOf<PlayerDetailEvent>()
        val collector = launch(UnconfinedTestDispatcher(testScheduler)) {
            viewModel.eventFlow.collect { events += it }
        }

        viewModel.sendIntent(PlayerDetailIntent.OnClickLyrics)

        assertEquals(PlayerArtworkFace.LYRICS_MISSING, viewModel.uiState.value.artworkFace)
        assertEquals(emptyList<PlayerDetailEvent>(), events)
        collector.cancel()
    }

    @Test
    fun `가사 없음 안내를 다시 누르면 아트워크로 돌아간다`() {
        val viewModel = viewModel()
        viewModel.sendIntent(PlayerDetailIntent.OnClickLyrics)

        viewModel.sendIntent(PlayerDetailIntent.OnClickLyrics)

        assertEquals(PlayerArtworkFace.ARTWORK, viewModel.uiState.value.artworkFace)
    }

    @Test
    fun `가사 찾기를 누르면 가사 검색으로 보낸다`() = runTest {
        val viewModel = viewModel()
        viewModel.sendIntent(PlayerDetailIntent.OnClickLyrics)
        val events = mutableListOf<PlayerDetailEvent>()
        val collector = launch(UnconfinedTestDispatcher(testScheduler)) {
            viewModel.eventFlow.collect { events += it }
        }

        viewModel.sendIntent(PlayerDetailIntent.OnClickLyricsSearchButton)

        assertEquals(
            listOf(
                PlayerDetailEvent.NavigateToLyricsSearch(
                    audioMediaId = 1,
                    query = "제목1",
                    durationSeconds = 51.0,
                    playbackDurationSeconds = 51.0,
                )
            ),
            events,
        )
        // 찾으러 간 뜻이 남아야 저장하고 돌아왔을 때 가사 면이 바로 뜬다.
        assertTrue(viewModel.uiState.value.isLyricsPanelVisible)
        collector.cancel()
    }

    @Test
    fun `가사를 찾으러 간 뒤 가사가 생기면 가사 면이 뜬다`() {
        val viewModel = viewModel()
        viewModel.sendIntent(PlayerDetailIntent.OnClickLyrics)
        viewModel.sendIntent(PlayerDetailIntent.OnClickLyricsSearchButton)

        givenSyncedLyrics()
        playing = mediaControlData("2")
        viewModel.sendIntent(PlayerDetailIntent.OnRefreshMediaData)

        assertEquals(PlayerArtworkFace.LYRICS, viewModel.uiState.value.artworkFace)
    }

    @Test
    fun `새 곡의 가사 유무를 모르는 동안에는 가사 면을 그대로 둔다`() {
        givenSyncedLyrics()
        val viewModel = viewModel()
        viewModel.sendIntent(PlayerDetailIntent.OnClickLyrics)
        assertEquals(PlayerArtworkFace.LYRICS, viewModel.uiState.value.artworkFace)

        // 저장소가 아직 새 곡의 가사 유무를 흘리지 않은 상태.
        every { fetchAudioMediaDetail.invoke(any()) } returns emptyFlow()
        playing = mediaControlData("2")
        viewModel.sendIntent(PlayerDetailIntent.OnRefreshMediaData)

        // 없다고 단정하면 여기서 아트워크로 샜다가 돌아와 앨범이 번쩍인다.
        assertEquals(PlayerArtworkFace.LYRICS, viewModel.uiState.value.artworkFace)
    }

    @Test
    fun `가사 유무를 모르는 동안 받은 탭은 답이 온 뒤에 맞는 면을 연다`() {
        givenSyncedLyrics()
        val viewModel = viewModel()
        // 새 곡의 답을 테스트가 쥐고 있다가 나중에 흘린다. 이전 곡은 가사가 있었다.
        val answer = MutableSharedFlow<List<PlaylistTrackAggregate>>(extraBufferCapacity = 1)
        every { fetchAudioMediaDetail.invoke(any()) } returns answer
        playing = mediaControlData("2")
        viewModel.sendIntent(PlayerDetailIntent.OnRefreshMediaData)

        viewModel.sendIntent(PlayerDetailIntent.OnClickLyrics)

        // 이전 곡의 답으로 열지 않는다.
        assertFalse(viewModel.uiState.value.showsLyricsFace)
        assertFalse(viewModel.uiState.value.isLyricsPanelVisible)

        // 이 곡에는 가사가 없다는 답이 온다.
        lyricFilePath = null
        parsedLines = emptyList()
        answer.tryEmit(listOf(aggregate()))

        assertEquals(PlayerArtworkFace.LYRICS_MISSING, viewModel.uiState.value.artworkFace)
        // 가사 있는 곡까지 따라가는 선호는 켜지지 않는다.
        assertFalse(viewModel.uiState.value.isLyricsPanelVisible)
    }

    @Test
    fun `이전 곡 가사가 떠 있는 동안 줄을 눌러도 옮기지 않는다`() {
        givenSyncedLyrics()
        val viewModel = viewModel()
        every { fetchAudioMediaDetail.invoke(any()) } returns emptyFlow()
        playing = mediaControlData("2")
        viewModel.sendIntent(PlayerDetailIntent.OnRefreshMediaData)

        viewModel.sendIntent(PlayerDetailIntent.OnClickLyricLine(2))

        verify(exactly = 0) { manager.seek(any()) }
    }

    @Test
    fun `가사 없음 안내를 띄운 채 다음 곡으로 넘어가면 아트워크로 돌아간다`() {
        val viewModel = viewModel()
        viewModel.sendIntent(PlayerDetailIntent.OnClickLyrics)
        assertEquals(PlayerArtworkFace.LYRICS_MISSING, viewModel.uiState.value.artworkFace)

        playing = mediaControlData("2")
        viewModel.sendIntent(PlayerDetailIntent.OnRefreshMediaData)

        assertEquals(PlayerArtworkFace.ARTWORK, viewModel.uiState.value.artworkFace)
        assertFalse(viewModel.uiState.value.isLyricsMissingNoticeVisible)
    }

    @Test
    fun `음원 정보를 고치면 재생 화면의 제목과 아티스트가 따라 바뀐다`() {
        // 재생기는 곡을 걸 때 찍힌 스냅샷을 그대로 들고 있다. 저장소가 다시 흘려보내야 바뀐다.
        every { fetchAudioMediaDetail.invoke(any()) } returns flowOf(
            listOf(aggregate(name = "제목1", artist = "옛 아티스트")),
            listOf(aggregate(name = "고친 제목", artist = "고친 아티스트")),
        )

        val viewModel = viewModel()

        assertEquals("고친 제목", viewModel.uiState.value.title)
        assertEquals("고친 아티스트", viewModel.uiState.value.artist)
    }

    @Test
    fun `어느 플레이리스트에도 없는 곡이면 재생기 표기를 지우지 않는다`() {
        every { fetchAudioMediaDetail.invoke(any()) } returns flowOf(emptyList())

        val viewModel = viewModel()

        assertEquals("제목1", viewModel.uiState.value.title)
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
        // Result 는 value class 라 relaxed mock 이 Object 를 담은 Result 를 만들어 캐스팅이 터진다.
        getPlaylistUseCase = mockk { coEvery { this@mockk(any()) } returns Result.failure(IllegalStateException()) },
        fetchAudioMediaDetailFlowUseCase = fetchAudioMediaDetail,
        toggleFavoriteUseCase = mockk(relaxed = true),
        getParsedLyricsUseCase = getParsedLyrics,
        getLyricsVariantUseCase = getLyricsVariant,
    )
}
