package com.kintmin.presentation.ui.setting.data_migration_send

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
import com.kintmin.presentation.ui.setting.data_migration_send.view.DataMigrationSendStatusView
import com.kintmin.presentation.ui.setting.dialog.SettingConfirmDialog
import com.kintmin.presentation.ui.setting.section.SettingRunningWorkView
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
    SettingConfirmDialog(
        isShow = isStopDialogVisible,
        title = "보내기를 중단할까요?",
        message = "iPhone이 아직 받고 있습니다. 이 화면을 나가면 iPhone에서 전송이 실패합니다.",
        dismissText = "계속 보내기",
        confirmText = "중단",
        onDismiss = { isStopDialogVisible = false },
        onConfirm = navigateToBack,
    )

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        topBar = {
            TopAppBar(
                title = { Text("다른 기기로 데이터 보내기") },
                navigationIcon = {
                    IconButton(onClick = { if (uiState.isSending) isStopDialogVisible = true else navigateToBack() }) {
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
            if (uiState.keepsScreenOn) {
                SettingWarningBannerView(
                    title = "iPhone이 다 받을 때까지 이 화면을 켜 두세요",
                    message = "이 화면을 나가거나 앱을 종료하면 iPhone의 전송이 실패합니다.",
                )
            }

            when (val step = uiState.step) {
                is DataMigrationSendStep.Blocked -> SettingRunningWorkView(
                    runningWorks = step.runningWorks,
                    onClickRecheck = { sendIntent(SettingDataMigrationSendIntent.OnClickRecheck) },
                )
                else -> DataMigrationSendStatusView(step = step, sendIntent = sendIntent)
            }

            SettingStepCardView(
                stepNumber = 1,
                title = "같은 Wi-Fi에 연결",
                description = "이 기기와 데이터를 받을 iPhone이 같은 Wi-Fi에 연결되어 있어야 합니다.",
            )
            SettingStepCardView(
                stepNumber = 2,
                title = "iPhone에서 가져오기 시작",
                description = "iPhone의 JellyTube에서 설정 → Android에서 데이터 가져오기를 엽니다.",
            )
            SettingStepCardView(
                stepNumber = 3,
                title = "연결 코드 입력",
                description = "위의 6자리 코드를 iPhone에 입력합니다. iPhone이 다 받을 때까지 이 화면을 열어 두세요.",
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
