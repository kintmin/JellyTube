package com.kintmin.presentation.ui.lyrics_detail

import androidx.lifecycle.SavedStateHandle
import androidx.navigation.toRoute
import com.kintmin.domain.lyrics.usecase.ApplyLyricsToAudioMediaUseCase
import com.kintmin.domain.lyrics.usecase.ParseLyricsUseCase
import com.kintmin.presentation.ui.lyrics_detail.navigation.LyricsDetailScreenRoute
import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkStatic
import io.mockk.unmockkStatic
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.collect
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

@OptIn(ExperimentalCoroutinesApi::class)
class LyricsDetailViewModelTest {

    private lateinit var route: LyricsDetailScreenRoute
    private val applyLyrics = mockk<ApplyLyricsToAudioMediaUseCase>()

    @Before
    fun setUp() {
        Dispatchers.setMain(UnconfinedTestDispatcher())
        mockkStatic("androidx.navigation.SavedStateHandleKt")
        every {
            any<SavedStateHandle>().toRoute(LyricsDetailScreenRoute::class, any())
        } answers { route }
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
        unmockkStatic("androidx.navigation.SavedStateHandleKt")
    }

    @Test
    fun `SYNC 가사는 시간과 문장을 행으로 표시한다`() {
        route = route(
            plainLyrics = "일반 가사",
            syncedLyrics = "[00:03.50]둘째\n[00:01.00]첫째",
        )

        val state = viewModel().uiState.value

        assertTrue(state.isSynced)
        assertEquals(
            listOf(
                LyricsDetailUiState.SyncedLine(1_000L, "첫째"),
                LyricsDetailUiState.SyncedLine(3_500L, "둘째"),
            ),
            state.syncedLines,
        )
    }

    @Test
    fun `일반 가사는 기존 본문을 유지한다`() {
        route = route(plainLyrics = "첫째\n둘째", syncedLyrics = "")

        val state = viewModel().uiState.value

        assertFalse(state.isSynced)
        assertEquals("첫째\n둘째", state.plainLyrics)
        assertTrue(state.syncedLines.isEmpty())
    }

    @Test
    fun `가사 적용에 성공하면 해당 음원의 가사 뷰어로 보낸다`() = runTest {
        route = route(plainLyrics = "가사", syncedLyrics = "")
        coEvery { applyLyrics.invoke(any(), any(), any(), any(), any(), any()) } returns Result.success("/lyrics/1.lrc")
        val viewModel = viewModel()
        val events = mutableListOf<LyricsDetailEvent>()
        val collector = launch(UnconfinedTestDispatcher(testScheduler)) {
            viewModel.eventFlow.collect { events += it }
        }

        viewModel.sendIntent(LyricsDetailIntent.OnClickApply)

        assertEquals(
            listOf(
                LyricsDetailEvent.ShowToast("가사를 적용했습니다."),
                LyricsDetailEvent.NavigateToLyricsViewer(7),
            ),
            events,
        )
        collector.cancel()
    }

    private fun viewModel() = LyricsDetailViewModel(
        savedStateHandle = SavedStateHandle(),
        applyLyricsToAudioMediaUseCase = applyLyrics,
        parseLyricsUseCase = ParseLyricsUseCase(),
    )

    private fun route(
        plainLyrics: String,
        syncedLyrics: String,
    ) = LyricsDetailScreenRoute(
        audioMediaId = 7,
        trackName = "제목",
        artistName = "아티스트",
        plainLyrics = plainLyrics,
        syncedLyrics = syncedLyrics,
    )
}
