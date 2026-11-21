package com.kintmin.presentation.ui.playlist_detail.list

import androidx.lifecycle.SavedStateHandle
import androidx.navigation.toRoute
import com.kintmin.domain.audio_track.model.PlaylistTrackAggregate
import com.kintmin.domain.audio_track.usecase.FetchAudioMediaListFlowUseCase
import com.kintmin.presentation.ui.playlist_detail.navigation.PlaylistDetailScreenRoute
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkStatic
import io.mockk.unmockkStatic
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableSharedFlow
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
class PlaylistDetailListViewModelTest {

    private val audioListFlow = MutableSharedFlow<List<PlaylistTrackAggregate>>()

    @Before
    fun setUp() {
        Dispatchers.setMain(UnconfinedTestDispatcher())
        // 호스트 JVM 에는 Bundle 구현이 없어 실제 toRoute 는 터진다.
        mockkStatic("androidx.navigation.SavedStateHandleKt")
        every {
            any<SavedStateHandle>().toRoute(PlaylistDetailScreenRoute::class, any())
        } returns PlaylistDetailScreenRoute(playlistId = 7)
    }

    @After
    fun tearDown() {
        unmockkStatic("androidx.navigation.SavedStateHandleKt")
        Dispatchers.resetMain()
    }

    private fun createViewModel() = PlaylistDetailListViewModel(
        savedStateHandle = SavedStateHandle(),
        fetchAudioMediaListFlowUseCase = mockk<FetchAudioMediaListFlowUseCase> {
            every { this@mockk(any()) } returns audioListFlow
        },
        deleteAudioTrackListUseCase = mockk(relaxed = true),
        updateTrackSequenceUseCase = mockk(relaxed = true),
        mediaControllerManager = mockk(relaxed = true),
        appLog = mockk(relaxed = true),
    )

    @Test
    fun `DB 가 답하기 전에는 빈 플레이리스트로 보이지 않는다`() = runTest(UnconfinedTestDispatcher()) {
        val viewModel = createViewModel()
        backgroundScope.launch { viewModel.uiState.collect {} }

        assertFalse(viewModel.uiState.value.isLoaded)
    }

    @Test
    fun `DB 가 빈 목록을 주면 빈 플레이리스트로 보인다`() = runTest(UnconfinedTestDispatcher()) {
        val viewModel = createViewModel()
        backgroundScope.launch { viewModel.uiState.collect {} }

        audioListFlow.emit(emptyList())

        assertTrue(viewModel.uiState.value.isLoaded)
        assertEquals(emptyList<PlaylistDetailListItemUiState>(), viewModel.uiState.value.audioList)
    }
}
