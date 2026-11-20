package com.kintmin.presentation.ui.playlist_edit.list

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.viewModelScope
import androidx.navigation.toRoute
import com.kintmin.domain.playlist.usecase.UpdatePlaylistDescriptionUseCase
import com.kintmin.domain.playlist.usecase.UpdatePlaylistTitleUseCase
import com.kintmin.presentation.ui.playlist_edit.navigation.PlaylistEditScreenRoute
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkStatic
import io.mockk.unmockkStatic
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.cancel
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Before
import org.junit.Test

class PlaylistEditListViewModelTest {

    private val updatePlaylistTitleUseCase = mockk<UpdatePlaylistTitleUseCase> {
        coEvery { this@mockk(any(), any()) } returns Result.success(Unit)
    }
    private val updatePlaylistDescriptionUseCase = mockk<UpdatePlaylistDescriptionUseCase> {
        coEvery { this@mockk(any(), any()) } returns Result.success(Unit)
    }

    @Before
    fun setUp() {
        Dispatchers.setMain(UnconfinedTestDispatcher())
        // 호스트 JVM 에는 Bundle 구현이 없어 실제 toRoute 는 터진다.
        mockkStatic("androidx.navigation.SavedStateHandleKt")
        every {
            any<SavedStateHandle>().toRoute(PlaylistEditScreenRoute::class, any())
        } returns PlaylistEditScreenRoute(playlistId = PLAYLIST_ID)
    }

    @After
    fun tearDown() {
        unmockkStatic("androidx.navigation.SavedStateHandleKt")
        Dispatchers.resetMain()
    }

    private fun createViewModel() = PlaylistEditListViewModel(
        savedStateHandle = SavedStateHandle(),
        fetchPlaylistFlowUseCase = mockk(relaxed = true),
        fetchAudioMediaListFlowUseCase = mockk(relaxed = true),
        updateTrackSequenceUseCase = mockk(relaxed = true),
        updatePlaylistDescriptionUseCase = updatePlaylistDescriptionUseCase,
        updatePlaylistTitleUseCase = updatePlaylistTitleUseCase,
        deleteAudioMediaListUseCase = mockk(relaxed = true),
        deleteAudioTrackListUseCase = mockk(relaxed = true),
        updatePlaylistImageUseCase = mockk(relaxed = true),
        resetPlaylistImageUseCase = mockk(relaxed = true),
    )

    @Test
    fun `제목을 고친 직후 화면을 나가도 마지막 입력이 저장된다`() {
        val viewModel = createViewModel()

        viewModel.sendIntent(PlaylistEditListIntent.OnEditPlaylistTitle("새 제"))
        viewModel.sendIntent(PlaylistEditListIntent.OnEditPlaylistTitle("새 제목"))
        viewModel.viewModelScope.cancel()

        coVerify { updatePlaylistTitleUseCase(PLAYLIST_ID, "새 제목") }
    }

    @Test
    fun `설명을 고친 직후 화면을 나가도 마지막 입력이 저장된다`() {
        val viewModel = createViewModel()

        viewModel.sendIntent(PlaylistEditListIntent.OnEditPlaylistDescription("새 설명"))
        viewModel.viewModelScope.cancel()

        coVerify { updatePlaylistDescriptionUseCase(PLAYLIST_ID, "새 설명") }
    }

    private companion object {
        const val PLAYLIST_ID = 7
    }
}
