package com.kintmin.presentation.ui.setting.data_migration_receive

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.ArrowBackIosNew
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
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
import com.kintmin.presentation.platform.KeepScreenOnEffect
import com.kintmin.presentation.theme.JellyTubeTheme
import com.kintmin.presentation.ui.setting.data_migration_receive.view.DataMigrationConnectView
import com.kintmin.presentation.ui.setting.data_migration_receive.view.DataMigrationResultView
import com.kintmin.presentation.ui.setting.data_migration_receive.view.DataMigrationStageListView
import com.kintmin.presentation.ui.setting.dialog.SettingConfirmDialog
import com.kintmin.presentation.ui.setting.section.SettingStepCardView
import com.kintmin.presentation.ui.setting.section.SettingWarningBannerView
import org.koin.compose.viewmodel.koinViewModel

@Composable
fun SettingDataMigrationReceiveScreen(
    navigateToBack: () -> Unit,
) {
    val viewModel = koinViewModel<SettingDataMigrationReceiveViewModel>()
    val uiState by viewModel.uiState.collectAsState()

    LaunchedEffect(Unit) {
        viewModel.eventFlow.collect { event ->
            when (event) {
                SettingDataMigrationReceiveEvent.NavigateToBack -> navigateToBack()
            }
        }
    }

    SettingDataMigrationReceiveScreen(
        uiState = uiState,
        navigateToBack = navigateToBack,
        sendIntent = viewModel::sendIntent,
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingDataMigrationReceiveScreen(
    uiState: SettingDataMigrationReceiveUiState,
    navigateToBack: () -> Unit,
    sendIntent: (SettingDataMigrationReceiveIntent) -> Unit,
) {
    KeepScreenOnEffect(enabled = uiState.isRunning)

    var isExitDialogVisible by remember { mutableStateOf(false) }
    val onBack = { if (uiState.exitConfirmation != null) isExitDialogVisible = true else navigateToBack() }
    NavigationBackHandler(
        state = rememberNavigationEventState(NavigationEventInfo.None),
        isBackEnabled = uiState.exitConfirmation != null,
    ) {
        isExitDialogVisible = true
    }
    uiState.exitConfirmation?.let { confirmation ->
        SettingConfirmDialog(
            isShow = isExitDialogVisible,
            title = confirmation.title(),
            message = confirmation.message(),
            dismissText = "계속하기",
            confirmText = "나가기",
            onDismiss = { isExitDialogVisible = false },
            onConfirm = { sendIntent(SettingDataMigrationReceiveIntent.OnConfirmExit) },
        )
    }

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        topBar = {
            TopAppBar(
                title = { Text("Android에서 데이터 가져오기") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            imageVector = Icons.Rounded.ArrowBackIosNew,
                            contentDescription = "뒤로가기",
                        )
                    }
                },
            )
        },
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            if (uiState.isRunning) {
                SettingWarningBannerView(
                    title = "화면을 잠그거나 앱을 나가지 마세요",
                    message = "잠그거나 다른 앱으로 가면 전송이 멈춥니다. 멈추면 '다시 시도'로 받은 데까지 이어 받습니다.",
                )
            }

            when (val phase = uiState.phase) {
                DataMigrationReceivePhase.Running,
                is DataMigrationReceivePhase.Interrupted,
                is DataMigrationReceivePhase.Finished,
                -> {
                    DataMigrationStageListView(stages = uiState.stages)
                    if (phase != DataMigrationReceivePhase.Running) {
                        DataMigrationResultView(
                            phase = phase,
                            onClickRetry = { sendIntent(SettingDataMigrationReceiveIntent.OnClickRetry) },
                            onClickExit = onBack,
                        )
                    }
                }
                else -> {
                    DataMigrationConnectView(uiState = uiState, sendIntent = sendIntent)
                    SettingStepCardView(
                        stepNumber = 1,
                        title = "Android에서 보내기 시작",
                        description = "Android의 JellyTube에서 설정 → 다른 기기로 데이터 보내기를 엽니다.",
                    )
                    SettingStepCardView(
                        stepNumber = 2,
                        title = "같은 Wi-Fi에서 기기 찾기",
                        description = "두 기기가 같은 Wi-Fi에 연결되어 있어야 합니다. 처음 찾을 때 로컬 네트워크 접근을 허용해 주세요.",
                    )
                    SettingStepCardView(
                        stepNumber = 3,
                        title = "코드 입력 후 기다리기",
                        description = "이미 있는 곡은 이 기기의 정보를 그대로 두고, 같은 이름의 재생목록은 하나로 합칩니다.",
                    )
                }
            }
        }
    }
}

private fun DataMigrationExitConfirmation.title(): String = when (this) {
    DataMigrationExitConfirmation.StopRunning -> "가져오기를 중단할까요?"
    DataMigrationExitConfirmation.StopInterrupted -> "가져오기를 그만둘까요?"
    is DataMigrationExitConfirmation.LeaveFailures -> "가져오지 못한 곡이 있습니다"
}

private fun DataMigrationExitConfirmation.message(): String = when (this) {
    DataMigrationExitConfirmation.StopRunning ->
        "아직 받는 중입니다. 나가면 받던 파일을 지우고, 이번 가져오기는 이 iPhone에 반영되지 않습니다."
    DataMigrationExitConfirmation.StopInterrupted ->
        "나가면 받아 둔 임시 파일을 지웁니다. 나중에 다시 가져오면 처음부터 받습니다."
    is DataMigrationExitConfirmation.LeaveFailures ->
        "옮기지 못한 ${failedCount}곡은 이 iPhone에 추가되지 않았습니다. 나중에 다시 가져오면 이 곡들만 옮길 수 있습니다."
}

@Preview
@Composable
private fun SettingDataMigrationReceiveScreenRunningPreview() {
    JellyTubeTheme {
        SettingDataMigrationReceiveScreen(
            uiState = SettingDataMigrationReceiveUiState.getMock(),
            navigateToBack = {},
            sendIntent = {},
        )
    }
}

@Preview
@Composable
private fun SettingDataMigrationReceiveScreenIdlePreview() {
    JellyTubeTheme {
        SettingDataMigrationReceiveScreen(
            uiState = SettingDataMigrationReceiveUiState(),
            navigateToBack = {},
            sendIntent = {},
        )
    }
}
