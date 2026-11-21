package com.kintmin.presentation.ui.setting

import com.kintmin.domain.backup.model.BackupFolder
import com.kintmin.domain.backup.model.BackupOutcome
import com.kintmin.domain.backup.model.BackupStatus
import com.kintmin.domain.backup.usecase.FetchBackupStatusFlowUseCase
import com.kintmin.domain.backup.usecase.HasLocalBackupUseCase
import com.kintmin.domain.backup.usecase.RegisterBackupFolderUseCase
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.take
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class SettingViewModelBackupTileTest {

    private val folder = BackupFolder("content://tree/1", "Download")
    private val status = mockk<FetchBackupStatusFlowUseCase>()
    private val register = mockk<RegisterBackupFolderUseCase>()
    private val hasLocalBackup = mockk<HasLocalBackupUseCase> { coEvery { this@mockk() } returns false }

    @Before
    fun setUp() {
        Dispatchers.setMain(UnconfinedTestDispatcher())
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    private fun viewModel() = SettingViewModel(
        fetchShouldInsertAtTopOnDownloadFlowUseCase = mockk(relaxed = true),
        fetchDownloadTargetPlaylistsFlowUseCase = mockk(relaxed = true),
        fetchIsStepEnabledFlowUseCase = mockk(relaxed = true),
        fetchUserIdUseCase = mockk { coEvery { this@mockk() } returns Result.success("id") },
        fetchBackupStatusFlowUseCase = status,
        registerBackupFolderUseCase = register,
        hasLocalBackupUseCase = hasLocalBackup,
        updateShouldInsertAtTopOnDownloadUseCase = mockk(relaxed = true),
        updatePlaylistIdOnDownloadUseCase = mockk(relaxed = true),
        updateIsStepEnabledUseCase = mockk(relaxed = true),
        executeAnomalyDataCheck = mockk(relaxed = true),
        importSharedAudioMediaUseCase = mockk(relaxed = true),
        stepMeasurementController = mockk(relaxed = true),
    )

    @Test
    fun `폴더를 고른 적이 없으면 타일이 폴더 선택기를 연다`() = runTest {
        every { status() } returns flowOf(BackupStatus(null, null, null, null))
        val viewModel = viewModel()

        viewModel.sendIntent(SettingIntent.OnClickBackupRestoreTile)

        assertEquals(SettingEvent.LaunchBackupFolderPicker, viewModel.eventFlow.first())
    }

    @Test
    fun `재설치로 폴더 설정만 사라지고 남은 백업이 있으면 선택기 없이 백업 화면으로 간다`() = runTest {
        every { status() } returns flowOf(BackupStatus(null, null, null, null))
        coEvery { hasLocalBackup() } returns true
        val viewModel = viewModel()

        viewModel.sendIntent(SettingIntent.OnClickBackupRestoreTile)

        assertEquals(SettingEvent.NavigateToBackupRestoreScreen, viewModel.eventFlow.first())
    }

    @Test
    fun `폴더가 있으면 타일이 백업 화면으로 간다`() = runTest {
        every { status() } returns flowOf(BackupStatus(folder, 1_000, null, null))
        val viewModel = viewModel()

        viewModel.sendIntent(SettingIntent.OnClickBackupRestoreTile)

        assertEquals(SettingEvent.NavigateToBackupRestoreScreen, viewModel.eventFlow.first())
    }

    @Test
    fun `폴더를 고르면 등록해 첫 백업을 쓰고 백업 화면으로 간다`() = runTest {
        every { status() } returns flowOf(BackupStatus(null, null, null, null))
        coEvery { register(folder, any()) } returns Result.success(BackupOutcome.WRITTEN)
        val viewModel = viewModel()

        viewModel.sendIntent(SettingIntent.OnBackupFolderPicked(folder.id, folder.label))

        val events = viewModel.eventFlow.take(2).toList()
        coVerify { register(folder, any()) }
        assertEquals(SettingEvent.NavigateToBackupRestoreScreen, events.last())
    }
}
