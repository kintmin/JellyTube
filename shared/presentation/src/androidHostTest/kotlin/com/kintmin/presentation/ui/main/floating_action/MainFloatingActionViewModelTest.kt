package com.kintmin.presentation.ui.main.floating_action

import com.kintmin.domain.app_setting.usecase.UpdatePlaylistIdOnDownloadUseCase
import com.kintmin.domain.playlist.model.DownloadTargetPlaylists
import com.kintmin.domain.playlist.usecase.AddNewPlaylistUseCase
import com.kintmin.domain.playlist.usecase.FetchDownloadTargetPlaylistsFlowUseCase
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class MainFloatingActionViewModelTest {

    private val updatePlaylistIdOnDownload = mockk<UpdatePlaylistIdOnDownloadUseCase> {
        coEvery { this@mockk.invoke(any()) } returns Result.success(Unit)
    }

    private val addNewPlaylist = mockk<AddNewPlaylistUseCase> {
        coEvery { this@mockk.invoke(any()) } returns Result.success(42)
    }

    private fun viewModel() = MainFloatingActionViewModel(
        fetchDownloadTargetPlaylistsFlowUseCase = mockk<FetchDownloadTargetPlaylistsFlowUseCase> {
            every { this@mockk.invoke() } returns flowOf(DownloadTargetPlaylists(targetId = null, selectable = emptyList()))
        },
        updatePlaylistIdOnDownloadUseCase = updatePlaylistIdOnDownload,
        addNewPlaylistUseCase = addNewPlaylist,
    )

    @Before
    fun setUp() {
        Dispatchers.setMain(UnconfinedTestDispatcher())
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `새로 만들기를 누르면 시트를 닫고 다이얼로그만 띄운다`() = runTest {
        val viewModel = viewModel()
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { viewModel.uiState.collect {} }
        viewModel.sendIntent(MainFloatingActionIntent.OnClickPlaylistButton)

        viewModel.sendIntent(MainFloatingActionIntent.OnClickCreatePlaylist)

        assertFalse(viewModel.uiState.value.isPlaylistBottomSheetVisible)
        assertTrue(viewModel.uiState.value.isCreatePlaylistDialogVisible)
    }

    @Test
    fun `만들지 않고 닫으면 시트로 돌아간다`() = runTest {
        val viewModel = viewModel()
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { viewModel.uiState.collect {} }
        viewModel.sendIntent(MainFloatingActionIntent.OnClickCreatePlaylist)

        viewModel.sendIntent(MainFloatingActionIntent.OnDismissCreatePlaylistDialog)

        assertFalse(viewModel.uiState.value.isCreatePlaylistDialogVisible)
        assertTrue(viewModel.uiState.value.isPlaylistBottomSheetVisible)
    }

    @Test
    fun `만들면 시트로 돌아가지 않고 만든 플레이리스트를 받을 곳으로 삼는다`() = runTest {
        val viewModel = viewModel()
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { viewModel.uiState.collect {} }
        viewModel.sendIntent(MainFloatingActionIntent.OnClickCreatePlaylist)

        // 다이얼로그는 만들기를 알린 뒤 스스로 닫는다
        viewModel.sendIntent(MainFloatingActionIntent.OnCreatePlaylist("새 목록"))
        viewModel.sendIntent(MainFloatingActionIntent.OnDismissCreatePlaylistDialog)

        assertFalse(viewModel.uiState.value.isCreatePlaylistDialogVisible)
        assertFalse(viewModel.uiState.value.isPlaylistBottomSheetVisible)
        coVerify(exactly = 1) { updatePlaylistIdOnDownload(42) }
    }

    @Test
    fun `재생목록 주소는 재생목록 받기로 가고 믹스는 그 영상만 받는다`() = runTest {
        val viewModel = viewModel()

        viewModel.sendIntent(MainFloatingActionIntent.OnClickDownload("https://m.youtube.com/playlist?list=PLx", 7))
        assertEquals(MainFloatingActionEvent.NavigateToPlaylistDownload("https://m.youtube.com/playlist?list=PLx"), viewModel.eventFlow.first())

        val mix = "https://m.youtube.com/watch?v=a&list=RDa"
        viewModel.sendIntent(MainFloatingActionIntent.OnClickDownload(mix, 7))
        assertEquals(MainFloatingActionEvent.Download(mix, 7), viewModel.eventFlow.first())
    }

    @Test
    fun `재생목록에서 연 영상은 무엇을 받을지 물은 뒤 고른 대로 보낸다`() = runTest {
        val viewModel = viewModel()
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { viewModel.uiState.collect {} }
        val url = "https://m.youtube.com/watch?v=a&list=PLx"

        viewModel.sendIntent(MainFloatingActionIntent.OnClickDownload(url, 7))
        assertEquals(url, viewModel.uiState.value.downloadChoiceUrl)

        viewModel.sendIntent(MainFloatingActionIntent.OnChooseSingleDownload)
        assertNull(viewModel.uiState.value.downloadChoiceUrl)
        assertEquals(MainFloatingActionEvent.Download(url, 7), viewModel.eventFlow.first())

        viewModel.sendIntent(MainFloatingActionIntent.OnClickDownload(url, 7))
        viewModel.sendIntent(MainFloatingActionIntent.OnChoosePlaylistDownload)
        assertEquals(MainFloatingActionEvent.NavigateToPlaylistDownload(url), viewModel.eventFlow.first())
    }
}
