package com.kintmin.presentation.ui.main

import androidx.lifecycle.SavedStateHandle
import androidx.navigation.toRoute
import com.kintmin.domain.app_setting.usecase.FetchHomeNoticeShownUseCase
import com.kintmin.domain.app_setting.usecase.HomeNoticeShown
import com.kintmin.domain.app_setting.usecase.MarkBackupNoticeShownUseCase
import com.kintmin.domain.app_setting.usecase.MarkNotificationNoticeShownUseCase
import com.kintmin.domain.backup.model.BackupFolder
import com.kintmin.domain.backup.model.BackupStatus
import com.kintmin.domain.backup.usecase.FetchBackupStatusFlowUseCase
import com.kintmin.presentation.ui.main.navigation.MainScreenRoute
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkStatic
import io.mockk.unmockkStatic
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class MainViewModelNoticeTest {

    private val folder = BackupFolder("content://tree/1", "Download/JellyTube")
    private val markNotification = mockk<MarkNotificationNoticeShownUseCase>(relaxed = true)
    private val markBackup = mockk<MarkBackupNoticeShownUseCase>(relaxed = true)

    @Before
    fun setUp() {
        Dispatchers.setMain(UnconfinedTestDispatcher())
        // 호스트 JVM 에는 Bundle 구현이 없어 실제 toRoute 는 터진다.
        mockkStatic("androidx.navigation.SavedStateHandleKt")
        every {
            any<SavedStateHandle>().toRoute(MainScreenRoute::class, any())
        } returns MainScreenRoute(MainTabItem.Playlist)
    }

    @After
    fun tearDown() {
        unmockkStatic("androidx.navigation.SavedStateHandleKt")
        Dispatchers.resetMain()
    }

    private fun viewModel(
        shown: HomeNoticeShown = HomeNoticeShown(notification = false, backup = false),
        backupFolder: BackupFolder? = null,
    ) = MainViewModel(
        savedStateHandle = SavedStateHandle(),
        checkAppUpdateOncePerDayUseCase = mockk(),
        ignoreAppUpdateUseCase = mockk(),
        fetchHomeNoticeShownUseCase = mockk<FetchHomeNoticeShownUseCase> {
            every { this@mockk() } returns flowOf(shown)
        },
        markNotificationNoticeShownUseCase = markNotification,
        markBackupNoticeShownUseCase = markBackup,
        fetchBackupStatusFlowUseCase = mockk<FetchBackupStatusFlowUseCase> {
            every { this@mockk() } returns flowOf(BackupStatus(backupFolder, null, null, null))
        },
    )

    @Test
    fun `아직 묻지 않았고 알림이 꺼져 있으면 먼저 설명한다`() {
        val viewModel = viewModel()

        viewModel.sendIntent(MainIntent.CheckNotificationNotice(isGranted = false))

        assertTrue(viewModel.uiState.value.isNotificationNoticeVisible)
    }

    @Test
    fun `이미 허용돼 있으면 묻지 않고 물어본 것으로 적는다`() {
        val viewModel = viewModel()

        viewModel.sendIntent(MainIntent.CheckNotificationNotice(isGranted = true))

        // 나중에 사용자가 알림을 꺼도 다시 묻지 않는다. 끈 것은 사용자의 선택이다.
        assertFalse(viewModel.uiState.value.isNotificationNoticeVisible)
        coVerify { markNotification() }
    }

    @Test
    fun `한 번 물었으면 권한이 없어도 다시 묻지 않는다`() {
        val viewModel = viewModel(shown = HomeNoticeShown(notification = true, backup = false))

        viewModel.sendIntent(MainIntent.CheckNotificationNotice(isGranted = false))

        assertFalse(viewModel.uiState.value.isNotificationNoticeVisible)
    }

    @Test
    fun `닫으면 다시 묻지 않도록 적는다`() {
        val viewModel = viewModel()
        viewModel.sendIntent(MainIntent.CheckNotificationNotice(isGranted = false))

        viewModel.sendIntent(MainIntent.CloseNotificationNotice)

        assertFalse(viewModel.uiState.value.isNotificationNoticeVisible)
        coVerify { markNotification() }
    }

    @Test
    fun `백업 폴더가 없고 아직 알리지 않았으면 배너를 보인다`() {
        assertTrue(viewModel().uiState.value.isBackupNoticeVisible)
    }

    @Test
    fun `폴더가 있거나 이미 알렸으면 배너를 보이지 않는다`() {
        assertFalse(viewModel(backupFolder = folder).uiState.value.isBackupNoticeVisible)
        assertFalse(viewModel(shown = HomeNoticeShown(notification = false, backup = true)).uiState.value.isBackupNoticeVisible)
    }

    @Test
    fun `배너를 닫으면 다시 보이지 않도록 적는다`() {
        val viewModel = viewModel()

        viewModel.sendIntent(MainIntent.DismissBackupNotice)

        assertFalse(viewModel.uiState.value.isBackupNoticeVisible)
        coVerify { markBackup() }
    }
}
