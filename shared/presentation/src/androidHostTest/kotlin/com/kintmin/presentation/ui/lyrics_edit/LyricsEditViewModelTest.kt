package com.kintmin.presentation.ui.lyrics_edit

import androidx.lifecycle.SavedStateHandle
import com.kintmin.domain.audio_track.model.PlaylistTrackAggregate
import com.kintmin.domain.audio_track.usecase.FetchAudioMediaDetailFlowUseCase
import com.kintmin.domain.lyrics.model.LyricsLine
import com.kintmin.domain.lyrics.usecase.GetLyricsVariantUseCase
import com.kintmin.domain.lyrics.usecase.GetParsedLyricsUseCase
import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkStatic
import io.mockk.unmockkStatic
import androidx.navigation.toRoute
import com.kintmin.presentation.ui.lyrics_edit.navigation.LyricsEditScreenRoute
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class LyricsEditViewModelTest {

    private var parsedLines: List<LyricsLine> = listOf(
        LyricsLine(timeMs = 1_000L, text = "첫 줄"),
        LyricsLine(timeMs = 5_000L, text = "둘째 줄"),
        LyricsLine(timeMs = 9_000L, text = "셋째 줄"),
    )

    private val aggregate = mockk<PlaylistTrackAggregate>(relaxed = true) {
        every { audioMedia.lyricFileFullPath } returns "/lyrics/7.lrc"
        every { audioMedia.name } returns "제목7"
        every { audioMedia.audioDuration } returns null
    }

    private fun viewModel() = LyricsEditViewModel(
        savedStateHandle = SavedStateHandle(),
        fetchAudioMediaDetailFlowUseCase = mockk<FetchAudioMediaDetailFlowUseCase> {
            every { this@mockk.invoke(any()) } returns flowOf(listOf(aggregate))
        },
        getParsedLyricsUseCase = mockk<GetParsedLyricsUseCase> {
            coEvery { this@mockk.invoke(any()) } answers { parsedLines }
        },
        getLyricsVariantUseCase = mockk<GetLyricsVariantUseCase> {
            coEvery { this@mockk.invoke(any(), any()) } returns Result.failure(IllegalStateException())
        },
        splitLyricsByNewlineUseCase = mockk(relaxed = true),
        saveEditedLyricsUseCase = mockk(relaxed = true),
    )

    @Before
    fun setUp() {
        Dispatchers.setMain(UnconfinedTestDispatcher())
        // 라우트 역직렬화가 android.os.Bundle 을 거쳐 호스트 테스트의 스텁에서 터진다.
        mockkStatic("androidx.navigation.SavedStateHandleKt")
        every {
            any<SavedStateHandle>().toRoute(LyricsEditScreenRoute::class, any())
        } returns LyricsEditScreenRoute(7)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
        unmockkStatic("androidx.navigation.SavedStateHandleKt")
    }

    @Test
    fun `지운 줄을 되돌리면 같은 자리에 같은 내용으로 돌아온다`() {
        val viewModel = viewModel()
        val before = viewModel.data.value.rows
        val deleted = before[1]

        viewModel.sendIntent(LyricsEditIntent.OnDeleteRow(deleted.id))
        assertEquals(2, viewModel.data.value.rows.size)

        viewModel.sendIntent(LyricsEditIntent.OnUndoDeleteRow)

        assertEquals(before, viewModel.data.value.rows)
    }

    @Test
    fun `되돌리기는 한 번만 적용된다`() {
        val viewModel = viewModel()
        viewModel.sendIntent(LyricsEditIntent.OnDeleteRow(viewModel.data.value.rows[0].id))

        viewModel.sendIntent(LyricsEditIntent.OnUndoDeleteRow)
        viewModel.sendIntent(LyricsEditIntent.OnUndoDeleteRow)

        assertEquals(3, viewModel.data.value.rows.size)
    }

    @Test
    fun `줄넘김이 든 줄이 있을 때만 쪼개기를 보인다`() {
        assertFalse(viewModel().data.value.canSplit)

        parsedLines = listOf(LyricsLine(timeMs = null, text = "첫 줄\n둘째 줄"))
        assertTrue(viewModel().data.value.canSplit)
    }
}
