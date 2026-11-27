package com.kintmin.presentation.ui.lyrics_search

import androidx.lifecycle.SavedStateHandle
import androidx.navigation.toRoute
import com.kintmin.domain.common_usecase.BuildSongSearchQueryUseCase
import com.kintmin.domain.lyrics.usecase.SearchLyricsUseCase
import com.kintmin.domain.lyrics.usecase.SortLyricsSearchResultsUseCase
import com.kintmin.domain.secret_mode.usecase.FetchSecretAudioMediaIdListFlowUseCase
import com.kintmin.presentation.secret_mode.ExternalSearchConsent
import com.kintmin.presentation.ui.lyrics_search.navigation.LyricsSearchScreenRoute
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkStatic
import io.mockk.unmockkStatic
import io.mockk.verify
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
    private val fetchSecretIds = mockk<FetchSecretAudioMediaIdListFlowUseCase> {
        every { this@mockk.invoke() } returns flowOf(emptyList())
    }

    private fun viewModel() = LyricsSearchViewModel(
        savedStateHandle = SavedStateHandle(),
        searchLyricsUseCase = searchLyrics,
        buildSongSearchQueryUseCase = buildQuery,
        sortLyricsSearchResultsUseCase = sortResults,
        externalSearchConsent = ExternalSearchConsent(fetchSecretIds),
    )

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
        val viewModel = viewModel()

        val state = viewModel.uiState.value

        assertEquals("음원 제목", state.audioMediaName)
        assertEquals("3:00", state.playbackDurationText)
        assertEquals("정리된 검색어", state.query)
        verify { sortResults.invoke(emptyList(), "정리된 검색어", 200.0) }
    }

    @Test
    fun `비밀 곡이면 묻기 전에 검색하지 않는다`() {
        every { fetchSecretIds.invoke() } returns flowOf(listOf(7))

        val viewModel = viewModel()

        assertTrue(viewModel.uiState.value.isConfirmingExternalSearch)
        // 검색을 건 적이 없으므로 "결과 없음" 을 뒤에 깔지 않는다.
        assertFalse(viewModel.uiState.value.isEmptyResult)
        coVerify(exactly = 0) { searchLyrics(any()) }
    }

    @Test
    fun `비밀 곡이어도 검색을 고르면 그때 검색한다`() {
        every { fetchSecretIds.invoke() } returns flowOf(listOf(7))
        val viewModel = viewModel()

        viewModel.sendIntent(LyricsSearchIntent.OnConfirmExternalSearch)

        assertFalse(viewModel.uiState.value.isConfirmingExternalSearch)
        coVerify(exactly = 1) { searchLyrics("정리된 검색어") }
    }

    @Test
    fun `한 번 고르면 다시 검색할 때는 묻지 않는다`() {
        every { fetchSecretIds.invoke() } returns flowOf(listOf(7))
        val viewModel = viewModel()
        viewModel.sendIntent(LyricsSearchIntent.OnConfirmExternalSearch)

        viewModel.sendIntent(LyricsSearchIntent.OnClickSearch)

        assertFalse(viewModel.uiState.value.isConfirmingExternalSearch)
        coVerify(exactly = 2) { searchLyrics("정리된 검색어") }
    }

    @Test
    fun `찾지 않기를 고르면 화면은 남고 다시 검색할 때 또 묻는다`() {
        every { fetchSecretIds.invoke() } returns flowOf(listOf(7))
        val viewModel = viewModel()

        viewModel.sendIntent(LyricsSearchIntent.OnDismissExternalSearch)
        assertFalse(viewModel.uiState.value.isConfirmingExternalSearch)

        viewModel.sendIntent(LyricsSearchIntent.OnClickSearch)

        assertTrue(viewModel.uiState.value.isConfirmingExternalSearch)
        coVerify(exactly = 0) { searchLyrics(any()) }
    }
}
