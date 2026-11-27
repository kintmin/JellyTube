package com.kintmin.presentation.ui.setting.backup_restore

import com.kintmin.domain.backup.model.BackupContent
import com.kintmin.domain.backup.model.BackupFileInvalidException
import com.kintmin.domain.backup.model.BackupOutcome
import com.kintmin.domain.backup.model.BackupStatus
import com.kintmin.domain.backup.model.RestorePreview
import com.kintmin.domain.backup.usecase.BackupNowUseCase
import com.kintmin.domain.backup.usecase.FetchBackupStatusFlowUseCase
import com.kintmin.domain.backup.usecase.FindBackupToRestoreUseCase
import com.kintmin.domain.backup.usecase.PreviewRestoreUseCase
import com.kintmin.domain.backup.usecase.RegisterBackupFolderUseCase
import com.kintmin.domain.backup.usecase.RestoreBackupUseCase
import com.kintmin.domain.data_migration.model.MigrationFailure
import com.kintmin.domain.data_migration.model.MigrationFailureReason
import com.kintmin.domain.data_migration.model.MigrationProgress
import com.kintmin.domain.data_migration.model.MigrationSnapshot
import com.kintmin.domain.data_migration.model.MigrationStage
import com.kintmin.domain.data_migration.model.MigrationSummary
import com.kintmin.domain.device.model.ConnectionStatus
import com.kintmin.domain.device.usecase.FetchConnectionStatusUseCase
import com.kintmin.presentation.port.BulkDownloadKeepAlive
import com.kintmin.presentation.port.RunningTaskMonitor
import com.kintmin.presentation.port.model.BulkDownloadTask
import com.kintmin.presentation.port.model.RunningTask
import com.kintmin.presentation.ui.setting.data_migration_receive.DataMigrationFailureItem
import com.kintmin.presentation.ui.setting.data_migration_receive.DataMigrationStageKind
import com.kintmin.presentation.ui.setting.data_migration_receive.DataMigrationStageStatus
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.CompletableDeferred
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
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class SettingBackupRestoreViewModelTest {

    private val backup = BackupContent(
        snapshot = MigrationSnapshot(emptyList(), emptyList(), emptyList(), emptyList(), emptyList()),
        downloadSettings = null,
        lyrics = emptyMap(),
        createdAtMillis = 0,
    )
    private val preview = RestorePreview(0, 10, 1, 8, 1, 32_000_000)
    private val find = mockk<FindBackupToRestoreUseCase> {
        coEvery { this@mockk() } returns Result.success(backup)
    }
    private val previewRestore = mockk<PreviewRestoreUseCase> {
        coEvery { this@mockk(backup) } returns Result.success(preview)
    }
    private val restore = mockk<RestoreBackupUseCase>()
    private val connection = mockk<FetchConnectionStatusUseCase> {
        every { this@mockk() } returns Result.success(ConnectionStatus(true, isWifi = true, isCellular = false, 0, 0))
    }
    private var status = BackupStatus(null, null, null, null)
    private val backupNow = mockk<BackupNowUseCase> {
        coEvery { this@mockk(any()) } returns Result.success(BackupOutcome.WRITTEN)
    }
    private val monitor = mockk<RunningTaskMonitor> {
        coEvery { runningTasks() } returns emptyMap()
    }

    @Before
    fun setUp() {
        Dispatchers.setMain(UnconfinedTestDispatcher())
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    private fun viewModel() = SettingBackupRestoreViewModel(
        fetchBackupStatusFlowUseCase = mockk<FetchBackupStatusFlowUseCase> {
            every { this@mockk() } returns flowOf(status)
        },
        backupNowUseCase = backupNow,
        registerBackupFolderUseCase = mockk<RegisterBackupFolderUseCase>(),
        findBackupToRestoreUseCase = find,
        previewRestoreUseCase = previewRestore,
        restoreBackupUseCase = restore,
        fetchConnectionStatusUseCase = connection,
        runningTaskMonitor = monitor,
        bulkDownloadKeepAlive = PassThroughKeepAlive,
    )

    @Test
    fun `화면을 열면 찾은 백업에서 무엇을 받을지 바로 보여 주고 복원은 누를 때만 한다`() {
        val viewModel = viewModel()

        val phase = viewModel.uiState.value.restore as RestorePhase.Preview
        assertEquals(RestorePreviewItem(createdAt = 0L.toDateTimeText(), 10, 1, 8, 1, 32), phase.preview)
        coVerify(exactly = 0) { restore(any(), any()) }
    }

    @Test
    fun `찾은 백업이 없으면 없다고 둔다`() {
        coEvery { find() } returns Result.success(null)
        val viewModel = viewModel()

        assertEquals(RestorePhase.NotFound, viewModel.uiState.value.restore)
    }

    @Test
    fun `폴더의 백업을 읽지 못하면 없다고 두지 않고 이유를 보인다`() {
        coEvery { find() } returns Result.failure(BackupFileInvalidException())
        val viewModel = viewModel()

        assertTrue(viewModel.uiState.value.restore is RestorePhase.ReadFailed)
    }

    @Test
    fun `Wi-Fi 가 아니면 먼저 묻고 계속하겠다고 해야 복원한다`() {
        every { connection() } returns Result.success(ConnectionStatus(true, isWifi = false, isCellular = true, 0, 0))
        coEvery { restore(backup, any()) } returns Result.success(MigrationSummary(8, 0, 8, emptyList()))
        val viewModel = viewModel()

        viewModel.sendIntent(SettingBackupRestoreIntent.OnClickStartRestore)

        assertTrue((viewModel.uiState.value.restore as RestorePhase.Preview).askCellular)
        coVerify(exactly = 0) { restore(any(), any()) }

        viewModel.sendIntent(SettingBackupRestoreIntent.OnConfirmCellularRestore)

        coVerify(exactly = 1) { restore(backup, any()) }
    }

    @Test
    fun `다시 받을 곡이 없으면 Wi-Fi 가 아니어도 묻지 않는다`() {
        every { connection() } returns Result.success(ConnectionStatus(true, isWifi = false, isCellular = true, 0, 0))
        coEvery { previewRestore(backup) } returns Result.success(preview.copy(alreadyOnDeviceCount = 9, redownloadCount = 0))
        coEvery { restore(backup, any()) } returns Result.success(MigrationSummary(0, 0, 0, emptyList()))
        val viewModel = viewModel()

        viewModel.sendIntent(SettingBackupRestoreIntent.OnClickStartRestore)

        coVerify(exactly = 1) { restore(backup, any()) }
    }

    @Test
    fun `복원이 끝나면 단계를 모두 마치고 실패한 곡을 이유와 함께 보인다`() {
        coEvery { restore(backup, any()) } coAnswers {
            secondArg<(MigrationProgress) -> Unit>()(MigrationProgress(MigrationStage.REDOWNLOAD, 3, 8))
            Result.success(MigrationSummary(7, 1, 7, listOf(MigrationFailure("녹음", MigrationFailureReason.NOT_RESTORABLE))))
        }
        val viewModel = viewModel()

        viewModel.sendIntent(SettingBackupRestoreIntent.OnClickStartRestore)

        val state = viewModel.uiState.value
        assertEquals(
            RestorePhase.Finished(7, 1, listOf(DataMigrationFailureItem("녹음", "원본이 유튜브 링크가 아니라 다시 받을 수 없음"))),
            state.restore,
        )
        assertTrue(state.stages.all { it.status == DataMigrationStageStatus.DONE })
        assertNull(state.exitConfirmation)
    }

    @Test
    fun `다시 받지 못한 곡이 있으면 다시 받기 단계를 실패로 두고 받은 곡만 센다`() {
        coEvery { restore(backup, any()) } coAnswers {
            secondArg<(MigrationProgress) -> Unit>()(MigrationProgress(MigrationStage.REDOWNLOAD, 8, 8))
            Result.success(
                MigrationSummary(
                    6, 0, 6,
                    listOf(
                        MigrationFailure("녹음", MigrationFailureReason.NOT_RESTORABLE),
                        MigrationFailure("비공개", MigrationFailureReason.REDOWNLOAD_FAILED),
                        MigrationFailure("삭제됨", MigrationFailureReason.REDOWNLOAD_FAILED),
                    ),
                ),
            )
        }
        val viewModel = viewModel()

        viewModel.sendIntent(SettingBackupRestoreIntent.OnClickStartRestore)

        val redownload = viewModel.uiState.value.stages.single { it.kind == DataMigrationStageKind.REDOWNLOAD }
        assertEquals(DataMigrationStageStatus.FAILED, redownload.status)
        assertEquals(6, redownload.doneCount)
    }

    @Test
    fun `결과를 닫으면 복원할 백업을 다시 찾는다`() {
        coEvery { restore(backup, any()) } returns Result.success(MigrationSummary(8, 0, 8, emptyList()))
        val viewModel = viewModel()
        viewModel.sendIntent(SettingBackupRestoreIntent.OnClickStartRestore)

        viewModel.sendIntent(SettingBackupRestoreIntent.OnClickCloseResult)

        assertTrue(viewModel.uiState.value.restore is RestorePhase.Preview)
        coVerify(exactly = 2) { find() }
    }

    @Test
    fun `다른 작업이 돌고 있으면 복원하지 않는다`() {
        coEvery { monitor.runningTasks() } returns mapOf(RunningTask.YOUTUBE_DOWNLOAD to 1)
        val viewModel = viewModel()

        viewModel.sendIntent(SettingBackupRestoreIntent.OnClickStartRestore)

        assertEquals(RestorePhase.Blocked(mapOf(RunningTask.YOUTUBE_DOWNLOAD to 1)), viewModel.uiState.value.restore)
        coVerify(exactly = 0) { restore(any(), any()) }
    }

    @Test
    fun `멈춘 뒤 다시 시도하면 같은 백업으로 다시 부른다`() {
        coEvery { restore(backup, any()) } returns Result.failure(IllegalStateException("끊김"))
        val viewModel = viewModel()
        viewModel.sendIntent(SettingBackupRestoreIntent.OnClickStartRestore)
        assertTrue(viewModel.uiState.value.restore is RestorePhase.Interrupted)

        viewModel.sendIntent(SettingBackupRestoreIntent.OnClickRetry)

        coVerify(exactly = 2) { restore(backup, any()) }
    }

    @Test
    fun `멈추면 화면에 남아 다시 시도할 수 있고 다시 시도하면 같은 백업으로 부른다`() = runTest {
        val never = CompletableDeferred<Result<MigrationSummary>>()
        coEvery { restore(backup, any()) } coAnswers { never.await() }
        val viewModel = viewModel()
        viewModel.sendIntent(SettingBackupRestoreIntent.OnClickStartRestore)

        viewModel.sendIntent(SettingBackupRestoreIntent.OnClickStop)

        assertTrue(viewModel.uiState.value.restore is RestorePhase.Interrupted)
        assertNull(viewModel.uiState.value.exitConfirmation)
        viewModel.sendIntent(SettingBackupRestoreIntent.OnClickRetry)
        coVerify(exactly = 2) { restore(backup, any()) }
    }

    @Test
    fun `멈춘 뒤 지금 백업을 눌러도 복원 구역은 그대로이고 다시 시도는 같은 백업으로 한다`() {
        coEvery { restore(backup, any()) } returns Result.failure(IllegalStateException("끊김"))
        val viewModel = viewModel()
        viewModel.sendIntent(SettingBackupRestoreIntent.OnClickStartRestore)

        viewModel.sendIntent(SettingBackupRestoreIntent.OnClickBackupNow)

        assertTrue(viewModel.uiState.value.restore is RestorePhase.Interrupted)
        coVerify(exactly = 1) { find() }
        viewModel.sendIntent(SettingBackupRestoreIntent.OnClickRetry)
        coVerify(exactly = 2) { restore(backup, any()) }
    }

    @Test
    fun `보관함에 반영하기 시작하면 멈출 수 없다`() = runTest {
        val never = CompletableDeferred<Result<MigrationSummary>>()
        coEvery { restore(backup, any()) } coAnswers {
            secondArg<(MigrationProgress) -> Unit>()(MigrationProgress(MigrationStage.APPLY))
            never.await()
        }
        val viewModel = viewModel()
        viewModel.sendIntent(SettingBackupRestoreIntent.OnClickStartRestore)

        assertFalse(viewModel.uiState.value.canStop)
        viewModel.sendIntent(SettingBackupRestoreIntent.OnClickStop)

        assertEquals(RestorePhase.Running, viewModel.uiState.value.restore)
    }

    @Test
    fun `복원이 끝나도 바로 백업하지 않는다 — 되돌린 백업을 지우지 않기 위해서다`() {
        coEvery { restore(backup, any()) } returns Result.success(MigrationSummary(8, 0, 8, emptyList()))
        val viewModel = viewModel()

        viewModel.sendIntent(SettingBackupRestoreIntent.OnClickStartRestore)

        coVerify(exactly = 0) { backupNow(any()) }
    }

    @Test
    fun `복원 중에 나가면 복원을 멈추고 화면을 닫는다`() = runTest {
        val never = CompletableDeferred<Result<MigrationSummary>>()
        coEvery { restore(backup, any()) } coAnswers { never.await() }
        val viewModel = viewModel()
        viewModel.sendIntent(SettingBackupRestoreIntent.OnClickStartRestore)
        assertEquals(RestoreExitConfirmation.StopRunning, viewModel.uiState.value.exitConfirmation)

        viewModel.sendIntent(SettingBackupRestoreIntent.OnConfirmExit)

        assertEquals(SettingBackupRestoreEvent.NavigateToBack, viewModel.eventFlow.first())
    }

    private object PassThroughKeepAlive : BulkDownloadKeepAlive {
        override suspend fun <T> keepAlive(task: BulkDownloadTask, block: suspend () -> T): T = block()
    }
}
