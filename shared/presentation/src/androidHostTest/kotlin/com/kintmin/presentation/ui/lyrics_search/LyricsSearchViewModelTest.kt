package com.kintmin.presentation.ui.lyrics_search

import androidx.lifecycle.SavedStateHandle
import androidx.navigation.toRoute
import com.kintmin.domain.common_usecase.BuildSongSearchQueryUseCase
import com.kintmin.domain.lyrics.usecase.SearchLyricsUseCase
import com.kintmin.domain.lyrics.usecase.SortLyricsSearchResultsUseCase
import com.kintmin.presentation.ui.lyrics_search.navigation.LyricsSearchScreenRoute
import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkStatic
import io.mockk.unmockkStatic
import io.mockk.verify
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class LyricsSearchViewModelTest {

    private val searchLyrics = mockk<SearchLyricsUseCase> {
        coEvery { this@mockk.invoke(any()) } returns Result.success(emptyList())
    }
    private val buildQuery = mockk<BuildSongSearchQueryUseCase> {
        every { this@mockk.invoke(any()) } returns "정리된 검색어"
    }
    private val sortResults = mockk<SortLyricsSearchResultsUseCase> {
        every { this@mockk.invoke(any(), any(), any()) } returns emptyList()
    }

    @Before
    fun setUp() {
        Dispatchers.setMain(UnconfinedTestDispatcher())
        mockkStatic("androidx.navigation.SavedStateHandleKt")
        every {
            any<SavedStateHandle>().toRoute(LyricsSearchScreenRoute::class, any())
        } returns LyricsSearchScreenRoute(
            audioMediaId = 7,
            initialQuery = "음원 제목",
            durationSeconds = 200.0,
            playbackDurationSeconds = 180.0,
        )
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
        unmockkStatic("androidx.navigation.SavedStateHandleKt")
    }

    @Test
    fun `검색 대상 음원 이름과 실제 재생 시간을 보여준다`() {
        val viewModel = LyricsSearchViewModel(
            savedStateHandle = SavedStateHandle(),
            searchLyricsUseCase = searchLyrics,
            buildSongSearchQueryUseCase = buildQuery,
            sortLyricsSearchResultsUseCase = sortResults,
        )

        val state = viewModel.uiState.value

        assertEquals("음원 제목", state.audioMediaName)
        assertEquals("00:03:00", state.playbackDurationText)
        assertEquals("정리된 검색어", state.query)
        verify { sortResults.invoke(emptyList(), "정리된 검색어", 200.0) }
    }
}
