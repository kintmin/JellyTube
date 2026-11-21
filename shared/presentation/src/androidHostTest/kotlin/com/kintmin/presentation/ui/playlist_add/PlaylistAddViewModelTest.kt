package com.kintmin.presentation.ui.playlist_add

import androidx.lifecycle.SavedStateHandle
import androidx.navigation.toRoute
import com.kintmin.domain.audio_track.usecase.FetchAudioMediaListToAddTrackFlowUseCase
import com.kintmin.domain.playlist.usecase.FetchPlaylistFlowUseCase
import com.kintmin.presentation.ui.playlist_add.navigation.PlaylistAddScreenRoute
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkStatic
import io.mockk.unmockkStatic
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test

class PlaylistAddViewModelTest {

    @Before
    fun setUp() {
        Dispatchers.setMain(UnconfinedTestDispatcher())
        // 호스트 JVM 에는 Bundle 구현이 없어 실제 toRoute 는 터진다.
        mockkStatic("androidx.navigation.SavedStateHandleKt")
        every {
            any<SavedStateHandle>().toRoute(PlaylistAddScreenRoute::class, any())
        } returns PlaylistAddScreenRoute(playlistId = PLAYLIST_ID)
    }

    @After
    fun tearDown() {
        unmockkStatic("androidx.navigation.SavedStateHandleKt")
        Dispatchers.resetMain()
    }

    private fun createViewModel() = PlaylistAddViewModel(
        savedStateHandle = SavedStateHandle(),
        fetchAudioMediaListFlowUseCase = mockk<FetchAudioMediaListToAddTrackFlowUseCase> {
            every { this@mockk(any()) } returns flowOf(emptyList())
        },
        fetchPlaylistFlowUseCase = mockk<FetchPlaylistFlowUseCase> {
            every { this@mockk(any()) } returns flowOf(mockk(relaxed = true))
        },
        addAudioMediaListToPlaylistUseCase = mockk(relaxed = true),
    )

    @Test
    fun `선택 해제는 고른 곡을 한 번에 모두 푼다`() = runTest(UnconfinedTestDispatcher()) {
        val viewModel = createViewModel()
        backgroundScope.launch { viewModel.uiState.collect {} }

        viewModel.sendIntent(PlaylistAddIntent.OnDragSelectItems(setOf(1, 2, 3)))
        assertEquals(3, viewModel.uiState.value.checkedItemCount)

        viewModel.sendIntent(PlaylistAddIntent.OnClickClearCheckedItemList)
        assertEquals(0, viewModel.uiState.value.checkedItemCount)
    }

    private companion object {
        const val PLAYLIST_ID = 7
    }
}
