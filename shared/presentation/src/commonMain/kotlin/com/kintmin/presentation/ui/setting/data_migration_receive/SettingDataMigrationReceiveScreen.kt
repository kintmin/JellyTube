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
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.kintmin.presentation.theme.JellyTubeTheme
import com.kintmin.presentation.ui.setting.data_migration_receive.view.DataMigrationReceiveStatusView
import com.kintmin.presentation.ui.setting.section.SettingStepCardView
import org.koin.compose.viewmodel.koinViewModel

@Composable
fun SettingDataMigrationReceiveScreen(
    navigateToBack: () -> Unit,
) {
    val viewModel = koinViewModel<SettingDataMigrationReceiveViewModel>()
    val uiState by viewModel.uiState.collectAsState()

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
    Scaffold(
        modifier = Modifier.fillMaxSize(),
        topBar = {
            TopAppBar(
                title = { Text("Android에서 데이터 가져오기") },
                navigationIcon = {
                    IconButton(onClick = navigateToBack) {
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
            DataMigrationReceiveStatusView(uiState = uiState, sendIntent = sendIntent)

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

@Preview
@Composable
private fun SettingDataMigrationReceiveScreenPreview() {
    JellyTubeTheme {
        SettingDataMigrationReceiveScreen(
            uiState = SettingDataMigrationReceiveUiState.getMock(),
            navigateToBack = {},
            sendIntent = {},
        )
    }
}
