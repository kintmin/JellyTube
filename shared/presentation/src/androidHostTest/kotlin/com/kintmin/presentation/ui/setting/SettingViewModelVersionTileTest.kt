package com.kintmin.presentation.ui.setting

import com.kintmin.domain.app_update.model.AppRelease
import com.kintmin.domain.app_update.usecase.CheckAppUpdateUseCase
import com.kintmin.domain.backup.model.BackupStatus
import com.kintmin.domain.playlist.model.DownloadTargetPlaylists
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.CompletableDeferred
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
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class SettingViewModelVersionTileTest {

    private val release = AppRelease(versionName = "2.1.0", downloadUrl = "https://example.com/app.apk")

    @Before
    fun setUp() {
        Dispatchers.setMain(UnconfinedTestDispatcher())
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    private fun viewModel(result: Result<AppRelease?>) =
        viewModel(mockk<CheckAppUpdateUseCase> { coEvery { this@mockk(any()) } returns result })

    // uiState 는 이 흐름들이 모두 한 번씩 내보내야 만들어진다.
    private fun viewModel(checkAppUpdate: CheckAppUpdateUseCase) = SettingViewModel(
        fetchShouldInsertAtTopOnDownloadFlowUseCase = mockk { every { this@mockk() } returns flowOf(false) },
        fetchDownloadTargetPlaylistsFlowUseCase = mockk {
            every { this@mockk() } returns flowOf(DownloadTargetPlaylists(targetId = null, selectable = emptyList()))
        },
        fetchIsStepEnabledFlowUseCase = mockk { every { this@mockk() } returns flowOf(false) },
        fetchUserIdUseCase = mockk { coEvery { this@mockk() } returns Result.success("id") },
        fetchBackupStatusFlowUseCase = mockk {
            every { this@mockk() } returns flowOf(BackupStatus(null, null, null, null))
        },
        registerBackupFolderUseCase = mockk(relaxed = true),
        updateShouldInsertAtTopOnDownloadUseCase = mockk(relaxed = true),
        updatePlaylistIdOnDownloadUseCase = mockk(relaxed = true),
        updateIsStepEnabledUseCase = mockk(relaxed = true),
        executeAnomalyDataCheck = mockk(relaxed = true),
        importSharedAudioMediaUseCase = mockk(relaxed = true),
        stepMeasurementController = mockk(relaxed = true),
        checkAppUpdateUseCase = checkAppUpdate,
    )

    @Test
    fun `확인하는 동안 진행 중으로 보이고 다시 눌러도 한 번만 확인한다`() = runTest(UnconfinedTestDispatcher()) {
        val gate = CompletableDeferred<Result<AppRelease?>>()
        val checkAppUpdate = mockk<CheckAppUpdateUseCase> { coEvery { this@mockk(any()) } coAnswers { gate.await() } }
        val viewModel = viewModel(checkAppUpdate)
        backgroundScope.launch { viewModel.uiState.collect {} }

        viewModel.sendIntent(SettingIntent.OnClickVersionTile("2.0.0"))
        viewModel.sendIntent(SettingIntent.OnClickVersionTile("2.0.0"))
        assertTrue(viewModel.uiState.value.isCheckingAppUpdate)

        gate.complete(Result.success(null))
        assertFalse(viewModel.uiState.value.isCheckingAppUpdate)
        coVerify(exactly = 1) { checkAppUpdate(any()) }
    }

    @Test
    fun `최신이면 토스트로 알린다`() = runTest {
        val viewModel = viewModel(Result.success(null))

        viewModel.sendIntent(SettingIntent.OnClickVersionTile("2.1.0"))

        assertEquals(SettingEvent.ShowToast("최신 버전입니다."), viewModel.eventFlow.first())
    }

    @Test
    fun `새 버전이 있으면 업데이트 팝업을 띄운다`() = runTest {
        val viewModel = viewModel(Result.success(release))

        viewModel.sendIntent(SettingIntent.OnClickVersionTile("2.0.0"))

        assertEquals(SettingEvent.ShowAppUpdateDialog(release), viewModel.eventFlow.first())
    }

    @Test
    fun `확인에 실패하면 최신이라고 말하지 않는다`() = runTest {
        val viewModel = viewModel(Result.failure(RuntimeException("offline")))

        viewModel.sendIntent(SettingIntent.OnClickVersionTile("2.0.0"))

        val event = viewModel.eventFlow.first()
        assertTrue(event is SettingEvent.ShowToast && event.message != "최신 버전입니다.")
    }
}
