package com.kintmin.presentation.ui.setting.data_migration_receive

import com.kintmin.domain.audio_media.usecase.CleanupAnomalousDataUseCase
import com.kintmin.domain.data_migration.model.MigrationEndpoint
import com.kintmin.domain.data_migration.model.MigrationFailure
import com.kintmin.domain.data_migration.model.MigrationFailureReason
import com.kintmin.domain.data_migration.model.MigrationPairRejectedException
import com.kintmin.domain.data_migration.model.MigrationProgress
import com.kintmin.domain.data_migration.model.MigrationSessionExpiredException
import com.kintmin.domain.data_migration.model.MigrationStage
import com.kintmin.domain.data_migration.model.MigrationSummary
import com.kintmin.domain.data_migration.usecase.ClearMigrationStagingUseCase
import com.kintmin.domain.data_migration.usecase.ImportMigrationDataUseCase
import com.kintmin.domain.data_migration.usecase.PairMigrationSourceUseCase
import com.kintmin.presentation.port.MigrationSourceDiscovery
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.coVerifyOrder
import io.mockk.mockk
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class SettingDataMigrationReceiveViewModelTest {

    private val endpoint = MigrationEndpoint("192.168.0.2", 52847)
    private val discovery = mockk<MigrationSourceDiscovery> {
        coEvery { discover() } returns endpoint
    }
    private val pair = mockk<PairMigrationSourceUseCase> {
        coEvery { this@mockk(endpoint, "123456") } returns Result.success("token")
    }
    private val import = mockk<ImportMigrationDataUseCase>()
    private val cleanup = mockk<CleanupAnomalousDataUseCase> {
        coEvery { this@mockk() } returns Result.success(0)
    }
    private val clearStaging = mockk<ClearMigrationStagingUseCase> {
        coEvery { this@mockk() } returns Result.success(Unit)
    }

    @Before
    fun setUp() {
        Dispatchers.setMain(UnconfinedTestDispatcher())
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    private fun viewModel() = SettingDataMigrationReceiveViewModel(discovery, pair, import, cleanup, clearStaging)

    private fun SettingDataMigrationReceiveViewModel.connect(code: String = "123456") {
        sendIntent(SettingDataMigrationReceiveIntent.OnClickSearch)
        sendIntent(SettingDataMigrationReceiveIntent.OnChangeCode(code))
        sendIntent(SettingDataMigrationReceiveIntent.OnClickConnect)
    }

    private fun summary(vararg failures: MigrationFailure) = MigrationSummary(3, 1, 0, failures.toList())

    @Test
    fun `기기를 못 찾으면 NotFound 다`() {
        coEvery { discovery.discover() } returns null
        val viewModel = viewModel()

        viewModel.sendIntent(SettingDataMigrationReceiveIntent.OnClickSearch)

        assertEquals(DataMigrationReceivePhase.NotFound, viewModel.uiState.value.phase)
    }

    @Test
    fun `코드는 숫자 여섯 자리까지만 받는다`() {
        val viewModel = viewModel()
        viewModel.sendIntent(SettingDataMigrationReceiveIntent.OnClickSearch)

        viewModel.sendIntent(SettingDataMigrationReceiveIntent.OnChangeCode("12a345678"))

        assertEquals("123456", viewModel.uiState.value.code)
        assertTrue(viewModel.uiState.value.canConnect)
    }

    @Test
    fun `페어링이 거부되면 코드를 비우고 안내와 함께 다시 입력받는다`() {
        coEvery { pair(endpoint, "000000") } returns Result.failure(MigrationPairRejectedException())
        val viewModel = viewModel()

        viewModel.connect("000000")

        val phase = viewModel.uiState.value.phase
        assertTrue(phase is DataMigrationReceivePhase.CodeInput && phase.errorMessage != null)
        assertEquals("", viewModel.uiState.value.code)
    }

    @Test
    fun `잔여물 정리 뒤 가져오고 실패한 곡을 사유와 함께 보여 준다`() {
        coEvery { import(endpoint, "token", any()) } returns Result.success(
            summary(MigrationFailure("녹음", MigrationFailureReason.UNSUPPORTED_FORMAT)),
        )
        val viewModel = viewModel()

        viewModel.connect()

        coVerifyOrder {
            cleanup()
            import(endpoint, "token", any())
        }
        val phase = viewModel.uiState.value.phase as DataMigrationReceivePhase.Finished
        assertEquals(listOf(DataMigrationFailureItem("녹음", "iPhone에서 재생할 수 없는 형식")), phase.failures)
        assertTrue(viewModel.uiState.value.stages.all { it.status == DataMigrationStageStatus.DONE })
        assertEquals(DataMigrationExitConfirmation.LeaveFailures(1), viewModel.uiState.value.exitConfirmation)
    }

    @Test
    fun `모두 가져오면 묻지 않고 나갈 수 있다`() {
        coEvery { import(endpoint, "token", any()) } returns Result.success(summary())
        val viewModel = viewModel()

        viewModel.connect()

        assertNull(viewModel.uiState.value.exitConfirmation)
    }

    @Test
    fun `받는 동안은 단계를 보여 주고 나가려면 묻는다`() {
        val gate = CompletableDeferred<Result<MigrationSummary>>()
        coEvery { import(endpoint, "token", any()) } coAnswers {
            thirdArg<(MigrationProgress) -> Unit>()(MigrationProgress(MigrationStage.TRANSFER, 1, 3, 10, 30))
            gate.await()
        }
        val viewModel = viewModel()

        viewModel.connect()

        val state = viewModel.uiState.value
        assertTrue(state.isRunning)
        assertEquals(DataMigrationExitConfirmation.StopRunning, state.exitConfirmation)
        val transfer = state.stages.first { it.kind == DataMigrationStageKind.TRANSFER }
        assertEquals(DataMigrationStageStatus.IN_PROGRESS, transfer.status)
        assertEquals(1, transfer.doneCount)
        assertTrue(state.stages.filter { it.kind.ordinal < transfer.kind.ordinal }.all { it.status == DataMigrationStageStatus.DONE })
        gate.complete(Result.success(summary()))
    }

    @Test
    fun `멈추면 진행 중이던 단계를 실패로 두고 다시 시도는 코드 없이 이어 받는다`() {
        coEvery { import(endpoint, "token", any()) } coAnswers {
            thirdArg<(MigrationProgress) -> Unit>()(MigrationProgress(MigrationStage.TRANSFER, 1, 3))
            Result.failure(IllegalStateException("끊김"))
        } coAndThen { Result.success(summary()) }
        val viewModel = viewModel()

        viewModel.connect()
        assertTrue(viewModel.uiState.value.phase is DataMigrationReceivePhase.Interrupted)
        assertEquals(
            DataMigrationStageStatus.FAILED,
            viewModel.uiState.value.stages.first { it.kind == DataMigrationStageKind.TRANSFER }.status,
        )

        viewModel.sendIntent(SettingDataMigrationReceiveIntent.OnClickRetry)

        assertTrue(viewModel.uiState.value.phase is DataMigrationReceivePhase.Finished)
        coVerify(exactly = 1) { pair(any(), any()) }
    }

    @Test
    fun `세션이 끝났으면 다시 시도 대신 새 코드를 받는다`() {
        coEvery { import(endpoint, "token", any()) } returns Result.failure(MigrationSessionExpiredException())
        coEvery { pair(endpoint, "654321") } returns Result.success("new-token")
        coEvery { import(endpoint, "new-token", any()) } returns Result.success(summary())
        val viewModel = viewModel()
        viewModel.connect()

        val phase = viewModel.uiState.value.phase
        assertTrue(phase is DataMigrationReceivePhase.CodeInput && phase.errorMessage != null)

        viewModel.sendIntent(SettingDataMigrationReceiveIntent.OnChangeCode("654321"))
        viewModel.sendIntent(SettingDataMigrationReceiveIntent.OnClickConnect)

        assertTrue(viewModel.uiState.value.phase is DataMigrationReceivePhase.Finished)
        coVerify(exactly = 1) { discovery.discover() }
    }

    @Test
    fun `멈춘 상태에서 나가면 임시 파일을 지우고 끝난 뒤 나가면 지우지 않는다`() = runTest {
        coEvery { import(endpoint, "token", any()) } returns Result.failure(IllegalStateException("끊김"))
        val interrupted = viewModel()
        interrupted.connect()

        interrupted.sendIntent(SettingDataMigrationReceiveIntent.OnConfirmExit)

        assertEquals(SettingDataMigrationReceiveEvent.NavigateToBack, interrupted.eventFlow.first())
        coVerify(exactly = 1) { clearStaging() }

        coEvery { import(endpoint, "token", any()) } returns Result.success(
            summary(MigrationFailure("곡", MigrationFailureReason.TRANSFER_FAILED)),
        )
        val finished = viewModel()
        finished.connect()

        finished.sendIntent(SettingDataMigrationReceiveIntent.OnConfirmExit)

        assertEquals(SettingDataMigrationReceiveEvent.NavigateToBack, finished.eventFlow.first())
        coVerify(exactly = 1) { clearStaging() }
    }
}
