package com.kintmin.presentation.ui.setting.data_migration_send

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Outbox
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
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
import com.kintmin.presentation.ui.common.ConfirmStyle
import com.kintmin.presentation.ui.common.DetailTopAppBar
import com.kintmin.presentation.ui.common.JellyTubeConfirmDialog
import com.kintmin.presentation.ui.setting.data_migration_send.view.DataMigrationSendStatusView
import com.kintmin.presentation.ui.setting.section.SettingNoticeCardView
import com.kintmin.presentation.ui.setting.section.SettingRunningTaskView
import com.kintmin.presentation.ui.setting.section.SettingStepCardView
import com.kintmin.presentation.ui.setting.section.SettingWarningBannerView
import org.koin.compose.viewmodel.koinViewModel

@Composable
fun SettingDataMigrationSendScreen(
    navigateToBack: () -> Unit,
) {
    val viewModel = koinViewModel<SettingDataMigrationSendViewModel>()
    val uiState by viewModel.uiState.collectAsState()

    SettingDataMigrationSendScreen(
        uiState = uiState,
        navigateToBack = navigateToBack,
        sendIntent = viewModel::sendIntent,
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingDataMigrationSendScreen(
    uiState: SettingDataMigrationSendUiState,
    navigateToBack: () -> Unit,
    sendIntent: (SettingDataMigrationSendIntent) -> Unit,
) {
    KeepScreenOnEffect(enabled = uiState.keepsScreenOn)

    var isStopDialogVisible by remember { mutableStateOf(false) }
    NavigationBackHandler(
        state = rememberNavigationEventState(NavigationEventInfo.None),
        isBackEnabled = uiState.isSending,
    ) {
        isStopDialogVisible = true
    }
    JellyTubeConfirmDialog(
        showDialog = isStopDialogVisible,
        title = "보내기를 중단할까요?",
        message = "받는 기기가 아직 받고 있습니다. 이 화면을 나가면 받는 기기의 전송이 실패합니다.",
        dismissText = "계속 보내기",
        confirmText = "나가기",
        confirmStyle = ConfirmStyle.Destructive,
        onDismiss = { isStopDialogVisible = false },
        onConfirm = navigateToBack,
    )

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        topBar = {
            DetailTopAppBar(
                title = uiState.title,
                onClickBack = { if (uiState.isSending) isStopDialogVisible = true else navigateToBack() },
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
            if (uiState.keepsScreenOn) {
                SettingWarningBannerView(
                    title = "받는 기기가 다 받을 때까지 이 화면을 켜 두세요",
                    message = "이 화면을 나가거나 앱을 종료하면 받는 기기의 전송이 실패합니다.",
                )
            }

            when (val step = uiState.step) {
                is DataMigrationSendStep.Blocked -> SettingRunningTaskView(
                    runningTasks = step.runningTasks,
                    onClickRecheck = { sendIntent(SettingDataMigrationSendIntent.OnClickRecheck) },
                )
                else -> DataMigrationSendStatusView(step = step, sendIntent = sendIntent)
            }

            SettingNoticeCardView(
                icon = Icons.Rounded.Outbox,
                title = "보낼 것",
                text = uiState.summary,
            )

            SettingStepCardView(
                stepNumber = 1,
                title = "같은 Wi-Fi에 연결",
                description = "이 기기와 데이터를 받을 기기가 같은 Wi-Fi에 연결되어 있어야 합니다.",
            )
            SettingStepCardView(
                stepNumber = 2,
                title = "받는 기기에서 가져오기 시작",
                description = "받는 기기의 JellyTube에서 설정 → 다른 기기에서 데이터 가져오기를 엽니다.",
            )
            SettingStepCardView(
                stepNumber = 3,
                title = "연결 코드 입력",
                description = "위의 6자리 코드를 받는 기기에 입력합니다. 받는 기기가 다 받을 때까지 이 화면을 열어 두세요.",
            )
        }
    }
}

@Preview
@Composable
private fun SettingDataMigrationSendScreenPreview() {
    JellyTubeTheme {
        SettingDataMigrationSendScreen(
            uiState = SettingDataMigrationSendUiState.getMock(),
            navigateToBack = {},
            sendIntent = {},
        )
    }
}

@Preview
@Composable
private fun SettingDataMigrationSendScreenSendingPreview() {
    JellyTubeTheme {
        SettingDataMigrationSendScreen(
            uiState = SettingDataMigrationSendUiState(step = DataMigrationSendStep.Sending("iPhone")),
            navigateToBack = {},
            sendIntent = {},
        )
    }
}
