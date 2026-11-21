package com.kintmin.presentation.ui.setting.backup_restore

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.navigationevent.NavigationEventInfo
import androidx.navigationevent.compose.NavigationBackHandler
import androidx.navigationevent.compose.rememberNavigationEventState
import com.kintmin.presentation.platform.AppPermission
import com.kintmin.presentation.platform.KeepScreenOnEffect
import com.kintmin.presentation.platform.rememberBackupFilePicker
import com.kintmin.presentation.platform.rememberBackupFolderPicker
import com.kintmin.presentation.platform.rememberPermissionController
import com.kintmin.presentation.platform.rememberToaster
import com.kintmin.presentation.theme.JellyTubeTheme
import com.kintmin.presentation.ui.common.ConfirmStyle
import com.kintmin.presentation.ui.common.DetailTopAppBar
import com.kintmin.presentation.ui.common.JellyTubeConfirmDialog
import com.kintmin.presentation.ui.setting.backup_restore.view.BackupSectionView
import com.kintmin.presentation.ui.setting.backup_restore.view.RestoreSectionView
import com.kintmin.presentation.ui.setting.section.SettingWarningBannerView
import org.koin.compose.viewmodel.koinViewModel

@Composable
fun SettingBackupRestoreScreen(
    navigateToBack: () -> Unit,
) {
    val viewModel = koinViewModel<SettingBackupRestoreViewModel>()
    val uiState by viewModel.uiState.collectAsState()
    val toaster = rememberToaster()
    val folderPicker = rememberBackupFolderPicker { folderId, label ->
        viewModel.sendIntent(SettingBackupRestoreIntent.OnBackupFolderPicked(folderId, label))
    }
    val filePicker = rememberBackupFilePicker { fileId ->
        viewModel.sendIntent(SettingBackupRestoreIntent.OnBackupFilePicked(fileId))
    }
    val permissionController = rememberPermissionController()

    LaunchedEffect(Unit) {
        if (!permissionController.isGranted(AppPermission.BackupStorage)) {
            permissionController.request(listOf(AppPermission.BackupStorage)) { result ->
                if (result[AppPermission.BackupStorage] == true) {
                    viewModel.sendIntent(SettingBackupRestoreIntent.OnBackupStorageGranted)
                }
            }
        }
    }
    LaunchedEffect(Unit) {
        viewModel.eventFlow.collect { event ->
            when (event) {
                SettingBackupRestoreEvent.NavigateToBack -> navigateToBack()
                SettingBackupRestoreEvent.LaunchBackupFolderPicker -> folderPicker.launch()
                SettingBackupRestoreEvent.LaunchBackupFilePicker -> filePicker.launch()
                is SettingBackupRestoreEvent.ShowToast -> toaster.show(event.message)
            }
        }
    }

    SettingBackupRestoreScreen(
        uiState = uiState,
        navigateToBack = navigateToBack,
        sendIntent = viewModel::sendIntent,
    )
}

@Composable
fun SettingBackupRestoreScreen(
    uiState: SettingBackupRestoreUiState,
    navigateToBack: () -> Unit,
    sendIntent: (SettingBackupRestoreIntent) -> Unit,
) {
    KeepScreenOnEffect(enabled = uiState.isRestoring)

    var isExitDialogVisible by remember { mutableStateOf(false) }
    val onBack = { if (uiState.exitConfirmation != null) isExitDialogVisible = true else navigateToBack() }
    NavigationBackHandler(
        state = rememberNavigationEventState(NavigationEventInfo.None),
        isBackEnabled = uiState.exitConfirmation != null,
    ) {
        isExitDialogVisible = true
    }
    if (uiState.exitConfirmation != null) {
        JellyTubeConfirmDialog(
            showDialog = isExitDialogVisible,
            title = "복원을 멈추고 나갈까요?",
            message = "받아 둔 곡은 남겨 두므로 다시 복원하면 이어 받습니다. 지금까지 받은 곡은 아직 보관함에 반영되지 않았습니다.",
            dismissText = "계속 받기",
            confirmText = "멈추고 나가기",
            confirmStyle = ConfirmStyle.Destructive,
            onDismiss = { isExitDialogVisible = false },
            onConfirm = { sendIntent(SettingBackupRestoreIntent.OnConfirmExit) },
        )
    }
    var isOverwriteDialogVisible by remember { mutableStateOf(false) }
    JellyTubeConfirmDialog(
        showDialog = isOverwriteDialogVisible,
        title = "남은 백업을 덮어쓸까요?",
        message = "아직 복원하지 않은 백업이 있습니다. 지금 백업하면 이 기기의 현재 상태로 바뀌어, 그 백업으로는 더 복원할 수 없습니다.",
        confirmText = "덮어쓰기",
        confirmStyle = ConfirmStyle.Destructive,
        onDismiss = { isOverwriteDialogVisible = false },
        onConfirm = { sendIntent(SettingBackupRestoreIntent.OnClickBackupNow) },
    )
    (uiState.restore as? RestorePhase.Preview)?.let { preview ->
        JellyTubeConfirmDialog(
            showDialog = preview.askCellular,
            title = "Wi-Fi에 연결되어 있지 않습니다",
            message = "곡 ${preview.preview.redownloadCount}개를 다시 받아 약 ${preview.preview.estimatedMegabytes} MB의 " +
                "모바일 데이터를 씁니다.",
            confirmText = "모바일 데이터로 복원",
            onDismiss = { sendIntent(SettingBackupRestoreIntent.OnDismissCellularRestore) },
            onConfirm = { sendIntent(SettingBackupRestoreIntent.OnConfirmCellularRestore) },
        )
    }

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        topBar = { DetailTopAppBar(title = "백업 및 복원", onClickBack = onBack) },
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .verticalScroll(rememberScrollState())
                .padding(vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            if (uiState.isRestoring) {
                SettingWarningBannerView(
                    modifier = Modifier.padding(horizontal = 16.dp),
                    title = "화면을 켜 두세요",
                    message = "앱을 나가면 받기가 멈출 수 있습니다. 멈추면 '다시 시도'로 받아 둔 곡부터 이어 받습니다.",
                )
            }
            BackupSectionView(
                uiState = uiState,
                onClickBackupNow = {
                    if (uiState.isAutoBackupPaused) isOverwriteDialogVisible = true
                    else sendIntent(SettingBackupRestoreIntent.OnClickBackupNow)
                },
                onClickPickFolder = { sendIntent(SettingBackupRestoreIntent.OnClickPickFolder) },
            )
            RestoreSectionView(
                uiState = uiState,
                onClickFindBackupFile = { sendIntent(SettingBackupRestoreIntent.OnClickFindBackupFile) },
                onClickPickFolder = { sendIntent(SettingBackupRestoreIntent.OnClickPickFolder) },
                onClickStart = { sendIntent(SettingBackupRestoreIntent.OnClickStartRestore) },
                onClickCancelPreview = { sendIntent(SettingBackupRestoreIntent.OnClickCancelPreview) },
                onClickStop = { sendIntent(SettingBackupRestoreIntent.OnClickStop) },
                onClickRetry = { sendIntent(SettingBackupRestoreIntent.OnClickRetry) },
                onClickCloseResult = { sendIntent(SettingBackupRestoreIntent.OnClickCloseResult) },
            )
        }
    }
}

@Preview
@Composable
private fun SettingBackupRestoreScreenPreview() {
    JellyTubeTheme {
        SettingBackupRestoreScreen(
            uiState = SettingBackupRestoreUiState.getMock(),
            navigateToBack = {},
            sendIntent = {},
        )
    }
}

@Preview
@Composable
private fun SettingBackupRestoreScreenRunningPreview() {
    JellyTubeTheme {
        SettingBackupRestoreScreen(
            uiState = SettingBackupRestoreUiState.getRunningMock(),
            navigateToBack = {},
            sendIntent = {},
        )
    }
}
