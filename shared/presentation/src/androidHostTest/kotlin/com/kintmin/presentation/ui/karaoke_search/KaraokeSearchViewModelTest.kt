package com.kintmin.presentation.ui.karaoke_search

import androidx.lifecycle.SavedStateHandle
import androidx.navigation.toRoute
import com.kintmin.domain.common_usecase.BuildSongSearchQueryUseCase
import com.kintmin.domain.karaoke.usecase.ApplyKaraokeNumberToAudioMediaUseCase
import com.kintmin.domain.karaoke.usecase.SearchKaraokeUseCase
import com.kintmin.domain.secret_mode.usecase.FetchSecretAudioMediaIdListFlowUseCase
import com.kintmin.presentation.secret_mode.ExternalSearchConsent
import com.kintmin.presentation.ui.karaoke_search.navigation.KaraokeSearchScreenRoute
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkStatic
import io.mockk.unmockkStatic
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOf
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
class KaraokeSearchViewModelTest {

    private val searchKaraoke = mockk<SearchKaraokeUseCase> {
        coEvery { this@mockk.invoke(any()) } returns Result.success(emptyList())
    }
    private val buildQuery = mockk<BuildSongSearchQueryUseCase> {
        every { this@mockk.invoke(any()) } returns "정리된 검색어"
    }
    private val applyNumber = mockk<ApplyKaraokeNumberToAudioMediaUseCase>()
    private val fetchSecretIds = mockk<FetchSecretAudioMediaIdListFlowUseCase> {
        every { this@mockk.invoke() } returns flowOf(emptyList())
    }

    private fun viewModel() = KaraokeSearchViewModel(
        savedStateHandle = SavedStateHandle(),
        searchKaraokeUseCase = searchKaraoke,
        buildSongSearchQueryUseCase = buildQuery,
        applyKaraokeNumberToAudioMediaUseCase = applyNumber,
        externalSearchConsent = ExternalSearchConsent(fetchSecretIds),
    )

    @Before
    fun setUp() {
        Dispatchers.setMain(UnconfinedTestDispatcher())
        mockkStatic("androidx.navigation.SavedStateHandleKt")
        every {
            any<SavedStateHandle>().toRoute(KaraokeSearchScreenRoute::class, any())
        } returns KaraokeSearchScreenRoute(audioMediaId = 7, initialQuery = "음원 제목")
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
        unmockkStatic("androidx.navigation.SavedStateHandleKt")
    }

    @Test
    fun `비밀 곡이 아니면 바로 검색한다`() {
        viewModel()

        coVerify(exactly = 1) { searchKaraoke("정리된 검색어") }
    }

    @Test
    fun `비밀 곡이면 묻기 전에 검색하지 않는다`() {
        every { fetchSecretIds.invoke() } returns flowOf(listOf(7))

        val viewModel = viewModel()

        assertTrue(viewModel.uiState.value.isConfirmingExternalSearch)
        // 검색을 건 적이 없으므로 "결과 없음" 을 뒤에 깔지 않는다.
        assertFalse(viewModel.uiState.value.isEmptyResult)
        coVerify(exactly = 0) { searchKaraoke(any()) }
    }

    @Test
    fun `비밀 곡이어도 검색을 고르면 그때 검색한다`() {
        every { fetchSecretIds.invoke() } returns flowOf(listOf(7))
        val viewModel = viewModel()

        viewModel.sendIntent(KaraokeSearchIntent.OnConfirmExternalSearch)

        assertFalse(viewModel.uiState.value.isConfirmingExternalSearch)
        coVerify(exactly = 1) { searchKaraoke("정리된 검색어") }
    }

    @Test
    fun `찾지 않기를 고르면 돌아간다`() = runTest {
        every { fetchSecretIds.invoke() } returns flowOf(listOf(7))
        val viewModel = viewModel()

        viewModel.sendIntent(KaraokeSearchIntent.OnDismissExternalSearch)

        assertEquals(KaraokeSearchEvent.NavigateBack, viewModel.eventFlow.first())
        coVerify(exactly = 0) { searchKaraoke(any()) }
    }
}
