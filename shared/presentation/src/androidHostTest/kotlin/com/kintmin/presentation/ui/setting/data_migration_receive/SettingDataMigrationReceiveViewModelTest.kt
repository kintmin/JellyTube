package com.kintmin.presentation.ui.setting.data_migration_receive

import com.kintmin.domain.data_migration.model.MigrationEndpoint
import com.kintmin.domain.data_migration.model.MigrationPairRejectedException
import com.kintmin.domain.data_migration.model.MigrationSummary
import com.kintmin.domain.data_migration.usecase.ImportMigrationDataUseCase
import com.kintmin.domain.data_migration.usecase.PairMigrationSourceUseCase
import com.kintmin.presentation.port.MigrationSourceDiscovery
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class SettingDataMigrationReceiveViewModelTest {

    private val endpoint = MigrationEndpoint("192.168.0.2", 52847)
    private val discovery = mockk<MigrationSourceDiscovery>()
    private val pair = mockk<PairMigrationSourceUseCase>()
    private val import = mockk<ImportMigrationDataUseCase>()

    @Before
    fun setUp() {
        Dispatchers.setMain(UnconfinedTestDispatcher())
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    private fun viewModel() = SettingDataMigrationReceiveViewModel(discovery, pair, import)

    private fun SettingDataMigrationReceiveViewModel.searchAndEnter(code: String) {
        sendIntent(SettingDataMigrationReceiveIntent.OnClickSearch)
        sendIntent(SettingDataMigrationReceiveIntent.OnChangeCode(code))
    }

    @Test
    fun `기기를 못 찾으면 NotFound 다`() {
        coEvery { discovery.discover() } returns null
        val viewModel = viewModel()

        viewModel.sendIntent(SettingDataMigrationReceiveIntent.OnClickSearch)

        assertEquals(DataMigrationReceiveStep.NotFound, viewModel.uiState.value.step)
    }

    @Test
    fun `코드는 숫자 여섯 자리까지만 받고 여섯 자리일 때만 연결할 수 있다`() {
        coEvery { discovery.discover() } returns endpoint
        val viewModel = viewModel()

        viewModel.searchAndEnter("12a34")
        assertEquals("1234", viewModel.uiState.value.code)
        assertTrue(!viewModel.uiState.value.canConnect)

        viewModel.sendIntent(SettingDataMigrationReceiveIntent.OnChangeCode("12345678"))
        assertEquals("123456", viewModel.uiState.value.code)
        assertTrue(viewModel.uiState.value.canConnect)
    }

    @Test
    fun `페어링이 거부되면 코드를 비우고 안내와 함께 다시 입력받는다`() {
        coEvery { discovery.discover() } returns endpoint
        coEvery { pair(endpoint, "123456") } returns Result.failure(MigrationPairRejectedException())
        val viewModel = viewModel()
        viewModel.searchAndEnter("123456")

        viewModel.sendIntent(SettingDataMigrationReceiveIntent.OnClickConnect)

        val step = viewModel.uiState.value.step
        assertTrue(step is DataMigrationReceiveStep.CodeInput && step.errorMessage != null)
        assertEquals("", viewModel.uiState.value.code)
    }

    @Test
    fun `페어링 후 옮기기를 마치면 결과를 보여 준다`() {
        coEvery { discovery.discover() } returns endpoint
        coEvery { pair(endpoint, "123456") } returns Result.success("token")
        coEvery { import(endpoint, "token", any()) } returns Result.success(MigrationSummary(3, 1, 0, emptyList()))
        val viewModel = viewModel()
        viewModel.searchAndEnter("123456")

        viewModel.sendIntent(SettingDataMigrationReceiveIntent.OnClickConnect)

        assertEquals(DataMigrationReceiveStep.Completed(3, 1), viewModel.uiState.value.step)
    }

    @Test
    fun `옮기다 실패하면 다시 시도할 때 코드 없이 같은 토큰으로 이어 받는다`() {
        coEvery { discovery.discover() } returns endpoint
        coEvery { pair(endpoint, "123456") } returns Result.success("token")
        coEvery { import(endpoint, "token", any()) } returnsMany listOf(
            Result.failure(IllegalStateException("끊김")),
            Result.success(MigrationSummary(3, 1, 0, emptyList())),
        )
        val viewModel = viewModel()
        viewModel.searchAndEnter("123456")
        viewModel.sendIntent(SettingDataMigrationReceiveIntent.OnClickConnect)
        assertTrue(viewModel.uiState.value.step is DataMigrationReceiveStep.Failed)

        viewModel.sendIntent(SettingDataMigrationReceiveIntent.OnClickRetry)

        assertEquals(DataMigrationReceiveStep.Completed(3, 1), viewModel.uiState.value.step)
        coVerify(exactly = 1) { pair(any(), any()) }
    }
}
