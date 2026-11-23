package com.kintmin.presentation.ui.setting.data_migration_send

import androidx.lifecycle.SavedStateHandle
import androidx.navigation.toRoute
import com.kintmin.presentation.port.DataMigrationSendController
import com.kintmin.presentation.port.RunningTaskMonitor
import com.kintmin.presentation.port.model.RunningTask
import com.kintmin.domain.secret_mode.usecase.FetchSecretModeEnabledFlowUseCase
import com.kintmin.presentation.secret_mode.SecretModeRunner
import com.kintmin.presentation.ui.setting.data_migration_send.navigation.SettingDataMigrationSendScreenRoute
import com.kintmin.presentation.ui.setting.data_migration_share.MigrationShareKind
import com.kintmin.domain.data_migration.model.DataMigrationSendStatus
import com.kintmin.domain.data_migration.model.MigrationScope
import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkStatic
import io.mockk.unmockkStatic
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
        every { start(any()) } answers { statusFlow.value = DataMigrationSendStatus.WaitingForPair("123456") }
    }

    private val monitor = mockk<RunningTaskMonitor> {
        coEvery { runningTasks() } returns emptyMap()
    }

    private val secretModeRunner = mockk<SecretModeRunner>(relaxUnitFun = true)

    private val isSecretModeEnabled = MutableStateFlow(false)
    private val fetchSecretModeEnabled = mockk<FetchSecretModeEnabledFlowUseCase> {
        every { this@mockk() } returns isSecretModeEnabled
    }

    private fun viewModel() = SettingDataMigrationSendViewModel(
        savedStateHandle = SavedStateHandle(),
        dataMigrationSendController = controller,
        runningWorkMonitor = monitor,
        secretModeRunner = secretModeRunner,
        fetchSecretModeEnabledFlowUseCase = fetchSecretModeEnabled,
    )

    @Before
    fun setUp() {
        Dispatchers.setMain(UnconfinedTestDispatcher())
        // 라우트 역직렬화가 android.os.Bundle 을 거쳐 호스트 테스트의 스텁에서 터진다.
        mockkStatic("androidx.navigation.SavedStateHandleKt")
        every {
            any<SavedStateHandle>().toRoute(SettingDataMigrationSendScreenRoute::class, any())
        } returns SettingDataMigrationSendScreenRoute(MigrationShareKind.SYNC)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
        unmockkStatic("androidx.navigation.SavedStateHandleKt")
    }

    @Test
    fun `화면에 들어오면 바로 연결 코드를 연다`() {
        val viewModel = viewModel()

        assertEquals(DataMigrationSendStep.WaitingForPair("123456"), viewModel.uiState.value.step)
        verify(exactly = 1) { controller.start(any()) }
    }

    @Test
    fun `다른 작업이 돌고 있으면 연결을 열지 않고 남은 작업을 보여 준다`() {
        val running = mapOf(RunningTask.YOUTUBE_DOWNLOAD to 2, RunningTask.PC_FILE_SHARE to 1)
        coEvery { monitor.runningTasks() } returns running

        val viewModel = viewModel()

        assertEquals(DataMigrationSendStep.Blocked(running), viewModel.uiState.value.step)
        verify(exactly = 0) { controller.start(any()) }

        coEvery { monitor.runningTasks() } returns emptyMap()
        viewModel.sendIntent(SettingDataMigrationSendIntent.OnClickRecheck)

        assertEquals(DataMigrationSendStep.WaitingForPair("123456"), viewModel.uiState.value.step)
    }

    @Test
    fun `받는 기기가 받는 동안만 뒤로가기를 막고 화면은 코드 대기부터 켜 둔다`() {
        val viewModel = viewModel()
        assertTrue(viewModel.uiState.value.keepsScreenOn)
        assertFalse(viewModel.uiState.value.isSending)

        statusFlow.value = DataMigrationSendStatus.Paired("iPhone")
        assertEquals(DataMigrationSendStep.Sending("iPhone"), viewModel.uiState.value.step)
        assertTrue(viewModel.uiState.value.isSending)
        assertTrue(viewModel.uiState.value.keepsScreenOn)

        statusFlow.value = DataMigrationSendStatus.Finished(newAudioMediaCount = 3, failedCount = 1)
        assertEquals(DataMigrationSendStep.Finished(3, 1), viewModel.uiState.value.step)
        assertFalse(viewModel.uiState.value.isSending)
        assertFalse(viewModel.uiState.value.keepsScreenOn)
    }

    @Test
    fun `비밀모드가 켜져 있을 때만 비밀 플레이리스트를 함께 보낸다`() {
        viewModel()
        verify { controller.start(MigrationScope.Everything(includesSecret = false)) }

        isSecretModeEnabled.value = true
        viewModel()
        verify { controller.start(MigrationScope.Everything(includesSecret = true)) }
    }

    @Test
    fun `고르는 화면에서 넘어왔으면 그 id 만 보낸다`() {
        every {
            any<SavedStateHandle>().toRoute(SettingDataMigrationSendScreenRoute::class, any())
        } returns SettingDataMigrationSendScreenRoute(MigrationShareKind.PLAYLIST, listOf(4, 5))

        val viewModel = viewModel()

        verify { controller.start(MigrationScope.Playlists(setOf(4, 5))) }
        assertEquals("고른 플레이리스트 2개와 그 안의 곡을 보냅니다.", viewModel.uiState.value.summary)
    }

    @Test
    fun `보내는 동안만 비밀모드 시계를 멈춘다`() {
        viewModel()

        statusFlow.value = DataMigrationSendStatus.Paired("iPhone")
        verify { secretModeRunner.pause() }

        statusFlow.value = DataMigrationSendStatus.Finished(newAudioMediaCount = 3, failedCount = 0)
        verify { secretModeRunner.resume() }
    }

    @Test
    fun `코드를 여러 번 틀려 잠기면 새 코드로 다시 연다`() {
        val viewModel = viewModel()

        statusFlow.value = DataMigrationSendStatus.Locked
        assertEquals(DataMigrationSendStep.Locked, viewModel.uiState.value.step)

        viewModel.sendIntent(SettingDataMigrationSendIntent.OnClickRenewCode)
        assertEquals(DataMigrationSendStep.WaitingForPair("123456"), viewModel.uiState.value.step)
    }

    @Test
    fun `화면을 떠나지 않았는데 서버가 내려가면 끊김을 보여 주고 새 코드로 다시 연다`() {
        val viewModel = viewModel()

        statusFlow.value = DataMigrationSendStatus.Idle
        assertEquals(DataMigrationSendStep.Disconnected, viewModel.uiState.value.step)

        viewModel.sendIntent(SettingDataMigrationSendIntent.OnClickRenewCode)
        assertEquals(DataMigrationSendStep.WaitingForPair("123456"), viewModel.uiState.value.step)
    }
}
