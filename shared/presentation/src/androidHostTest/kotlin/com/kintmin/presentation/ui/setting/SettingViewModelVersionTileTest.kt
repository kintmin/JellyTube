package com.kintmin.presentation.ui.setting

import com.kintmin.domain.app_update.model.AppRelease
import com.kintmin.domain.app_update.usecase.CheckAppUpdateUseCase
import io.mockk.coEvery
import io.mockk.mockk
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
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

    private fun viewModel(result: Result<AppRelease?>) = SettingViewModel(
        fetchShouldInsertAtTopOnDownloadFlowUseCase = mockk(relaxed = true),
        fetchDownloadTargetPlaylistsFlowUseCase = mockk(relaxed = true),
        fetchIsStepEnabledFlowUseCase = mockk(relaxed = true),
        fetchUserIdUseCase = mockk { coEvery { this@mockk() } returns Result.success("id") },
        fetchBackupStatusFlowUseCase = mockk(relaxed = true),
        registerBackupFolderUseCase = mockk(relaxed = true),
        hasLocalBackupUseCase = mockk(relaxed = true),
        updateShouldInsertAtTopOnDownloadUseCase = mockk(relaxed = true),
        updatePlaylistIdOnDownloadUseCase = mockk(relaxed = true),
        updateIsStepEnabledUseCase = mockk(relaxed = true),
        executeAnomalyDataCheck = mockk(relaxed = true),
        importSharedAudioMediaUseCase = mockk(relaxed = true),
        stepMeasurementController = mockk(relaxed = true),
        checkAppUpdateUseCase = mockk<CheckAppUpdateUseCase> { coEvery { this@mockk(any()) } returns result },
    )

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
