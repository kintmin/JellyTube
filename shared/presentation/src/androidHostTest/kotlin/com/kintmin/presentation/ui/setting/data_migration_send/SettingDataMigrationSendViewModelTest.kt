package com.kintmin.presentation.ui.setting.data_migration_send

import com.kintmin.presentation.port.DataMigrationSendController
import com.kintmin.presentation.port.model.DataMigrationSendStatus
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
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class SettingDataMigrationSendViewModelTest {

    private val statusFlow = MutableStateFlow<DataMigrationSendStatus>(DataMigrationSendStatus.Idle)
    private val controller = mockk<DataMigrationSendController>(relaxUnitFun = true) {
        every { status } returns statusFlow
        every { start() } answers { statusFlow.value = DataMigrationSendStatus.WaitingForPair("123456") }
    }

    @Before
    fun setUp() {
        Dispatchers.setMain(UnconfinedTestDispatcher())
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `화면에 들어오면 바로 연결 코드를 연다`() {
        val viewModel = SettingDataMigrationSendViewModel(controller)

        assertEquals(DataMigrationSendStep.WaitingForPair("123456"), viewModel.uiState.value.step)
        verify(exactly = 1) { controller.start() }
    }

    @Test
    fun `받는 기기가 받는 동안만 뒤로가기를 막고 화면은 코드 대기부터 켜 둔다`() {
        val viewModel = SettingDataMigrationSendViewModel(controller)
        assertTrue(viewModel.uiState.value.keepsScreenOn)
        assertFalse(viewModel.uiState.value.isSending)

        statusFlow.value = DataMigrationSendStatus.Paired
        assertTrue(viewModel.uiState.value.isSending)
        assertTrue(viewModel.uiState.value.keepsScreenOn)

        statusFlow.value = DataMigrationSendStatus.Finished(newAudioMediaCount = 3, failedCount = 1)
        assertEquals(DataMigrationSendStep.Finished(3, 1), viewModel.uiState.value.step)
        assertFalse(viewModel.uiState.value.isSending)
        assertFalse(viewModel.uiState.value.keepsScreenOn)
    }

    @Test
    fun `코드를 여러 번 틀려 잠기면 새 코드로 다시 연다`() {
        val viewModel = SettingDataMigrationSendViewModel(controller)

        statusFlow.value = DataMigrationSendStatus.Locked
        assertEquals(DataMigrationSendStep.Locked, viewModel.uiState.value.step)

        viewModel.sendIntent(SettingDataMigrationSendIntent.OnClickRenewCode)
        assertEquals(DataMigrationSendStep.WaitingForPair("123456"), viewModel.uiState.value.step)
    }
}
