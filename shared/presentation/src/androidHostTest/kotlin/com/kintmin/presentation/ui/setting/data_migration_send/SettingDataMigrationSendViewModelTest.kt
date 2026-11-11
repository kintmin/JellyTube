package com.kintmin.presentation.ui.setting.data_migration_send

import com.kintmin.domain.audio_media.usecase.CleanupAnomalousDataUseCase
import com.kintmin.presentation.port.DataMigrationSendController
import com.kintmin.presentation.port.model.DataMigrationSendStatus
import io.mockk.coEvery
import io.mockk.coVerifyOrder
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class SettingDataMigrationSendViewModelTest {

    private val controller = mockk<DataMigrationSendController>(relaxUnitFun = true) {
        every { status } returns MutableStateFlow(DataMigrationSendStatus.WaitingForPair("123456"))
    }
    private val cleanup = mockk<CleanupAnomalousDataUseCase>()

    @Before
    fun setUp() {
        Dispatchers.setMain(UnconfinedTestDispatcher())
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `이상 데이터 정리를 마친 뒤에 연결을 연다`() {
        coEvery { cleanup() } returns Result.success(0)

        val viewModel = SettingDataMigrationSendViewModel(controller, cleanup)

        assertEquals(DataMigrationSendStep.Ready, viewModel.uiState.value.step)
        coVerifyOrder {
            cleanup()
            controller.start()
        }
    }

    @Test
    fun `정리에 실패하면 연결을 열지 않고 다시 시도할 수 있다`() {
        coEvery { cleanup() } returnsMany listOf(Result.failure(IllegalStateException()), Result.success(0))

        val viewModel = SettingDataMigrationSendViewModel(controller, cleanup)
        assertEquals(DataMigrationSendStep.PreparationFailed, viewModel.uiState.value.step)
        verify(exactly = 0) { controller.start() }

        viewModel.sendIntent(SettingDataMigrationSendIntent.OnClickRetryPreparation)

        assertEquals(DataMigrationSendStep.Ready, viewModel.uiState.value.step)
        verify(exactly = 1) { controller.start() }
    }
}
