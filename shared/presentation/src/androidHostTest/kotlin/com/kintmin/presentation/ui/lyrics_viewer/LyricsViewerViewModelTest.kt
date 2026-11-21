package com.kintmin.presentation.ui.lyrics_viewer

import androidx.lifecycle.SavedStateHandle
import com.kintmin.domain.audio_track.model.PlaylistTrackAggregate
import com.kintmin.domain.audio_track.usecase.FetchAudioMediaDetailFlowUseCase
import com.kintmin.domain.lyrics.model.LyricsLine
import com.kintmin.domain.lyrics.model.LyricsVariant
import com.kintmin.domain.lyrics.usecase.DetectLyricsLanguageUseCase
import com.kintmin.domain.lyrics.usecase.GetLyricsVariantUseCase
import com.kintmin.domain.lyrics.usecase.GetParsedLyricsUseCase
import com.kintmin.presentation.port.PlaybackController
import com.kintmin.presentation.port.StartLyricsVariantCreation
import com.kintmin.presentation.port.model.MediaControlData
import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkStatic
import io.mockk.unmockkStatic
import io.mockk.verify
import androidx.navigation.toRoute
import com.kintmin.presentation.ui.lyrics_viewer.navigation.LyricsViewerScreenRoute
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import kotlin.time.Duration.Companion.milliseconds

@OptIn(ExperimentalCoroutinesApi::class)
class LyricsViewerViewModelTest {

    private val audioMediaId = 7

    private fun mediaControlData(mediaId: String) = MediaControlData(
        mediaId = mediaId,
        mediaFileUri = "/audios/$mediaId.m4a",
        mediaTitle = "제목$mediaId",
        mediaDescription = "",
        mediaArtist = "",
        mediaDurationMs = 51_000L,
        mediaArtworkFileUri = null,
    )

    private var playing: MediaControlData? = mediaControlData(audioMediaId.toString())
    private var isPlaying = true

    private var parsedLines: List<LyricsLine> = listOf(
        LyricsLine(timeMs = 1_000L, text = "첫 줄"),
        LyricsLine(timeMs = 5_000L, text = "둘째 줄"),
    )

    private val controller = mockk<PlaybackController>(relaxed = true) {
        every { this@mockk.isPlaying } answers { this@LyricsViewerViewModelTest.isPlaying }
        every { playingMediaItem } answers { playing }
        every { currentPosition } returns 1_500L
    }

    private val aggregate = mockk<PlaylistTrackAggregate>(relaxed = true) {
        every { audioMedia.lyricFileFullPath } returns "/lyrics/7.lrc"
        every { audioMedia.name } returns "제목7"
    }

    private val startVariantCreation = mockk<StartLyricsVariantCreation>(relaxed = true)

    private fun viewModel() = LyricsViewerViewModel(
        savedStateHandle = SavedStateHandle(),
        fetchAudioMediaDetailFlowUseCase = mockk<FetchAudioMediaDetailFlowUseCase> {
            every { this@mockk.invoke(any()) } returns flowOf(listOf(aggregate))
        },
        getParsedLyricsUseCase = mockk<GetParsedLyricsUseCase> {
            coEvery { this@mockk.invoke(any()) } answers { parsedLines }
        },
        deleteAudioMediaLyricsUseCase = mockk(relaxed = true),
        detectLyricsLanguageUseCase = mockk<DetectLyricsLanguageUseCase>(relaxed = true),
        getLyricsVariantUseCase = mockk<GetLyricsVariantUseCase> {
            coEvery { this@mockk.invoke(any(), any()) } returns Result.failure(IllegalStateException())
        },
        executeLyricsVariantCreation = startVariantCreation,
        mediaControllerManager = controller,
    )

    @Before
    fun setUp() {
        Dispatchers.setMain(UnconfinedTestDispatcher())
        // 라우트 역직렬화가 android.os.Bundle 을 거쳐 호스트 테스트의 스텁에서 터진다.
        mockkStatic("androidx.navigation.SavedStateHandleKt")
        every {
            any<SavedStateHandle>().toRoute(LyricsViewerScreenRoute::class, any())
        } returns LyricsViewerScreenRoute(audioMediaId)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
        unmockkStatic("androidx.navigation.SavedStateHandleKt")
    }

    @Test
    fun `이 곡이 재생 중이면 줄을 눌러 그 줄의 시간으로 이동한다`() {
        val viewModel = viewModel()
        viewModel.sendIntent(LyricsViewerIntent.OnRefreshPosition)
        assertTrue(viewModel.uiState.value.canSeek)

        viewModel.sendIntent(LyricsViewerIntent.OnClickLine(1))

        verify(exactly = 1) { controller.seek(5_000L.milliseconds) }
    }

    @Test
    fun `다른 곡이 재생 중이면 줄을 눌러도 이동하지 않는다`() {
        playing = mediaControlData("99")
        val viewModel = viewModel()
        viewModel.sendIntent(LyricsViewerIntent.OnRefreshPosition)

        viewModel.sendIntent(LyricsViewerIntent.OnClickLine(1))

        assertFalse(viewModel.uiState.value.canSeek)
        verify(exactly = 0) { controller.seek(any()) }
    }

    @Test
    fun `싱크가 없는 가사는 재생 중이어도 누를 수 없다`() {
        parsedLines = listOf(LyricsLine(timeMs = null, text = "첫 줄"))
        val viewModel = viewModel()
        viewModel.sendIntent(LyricsViewerIntent.OnRefreshPosition)

        viewModel.sendIntent(LyricsViewerIntent.OnClickLine(0))

        assertFalse(viewModel.uiState.value.canSeek)
        verify(exactly = 0) { controller.seek(any()) }
    }

    @Test
    fun `번역을 요청한 뒤 다시 눌러도 작업을 한 번만 건다`() {
        val viewModel = viewModel()

        viewModel.sendIntent(LyricsViewerIntent.OnClickCreateTranslation)
        viewModel.sendIntent(LyricsViewerIntent.OnClickCreateTranslation)

        assertTrue(viewModel.uiState.value.isCreatingTranslation)
        verify(exactly = 1) {
            startVariantCreation(
                audioMediaId = audioMediaId,
                lyricFileFullPath = any(),
                variant = LyricsVariant.TRANSLATION,
                sourceLanguage = any(),
            )
        }
    }
}
