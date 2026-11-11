package com.kintmin.presentation.ui.setting.data_migration_send

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.ArrowBackIosNew
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.kintmin.presentation.theme.JellyTubeTheme
import com.kintmin.presentation.ui.setting.section.SettingStepCardView
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
    Scaffold(
        modifier = Modifier.fillMaxSize(),
        topBar = {
            TopAppBar(
                title = { Text("다른 기기로 데이터 보내기") },
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
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer),
                shape = RoundedCornerShape(12.dp),
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    val code = uiState.pairingCode
                    if (code != null) {
                        Text(
                            text = "연결 코드",
                            style = MaterialTheme.typography.titleSmall,
                            color = MaterialTheme.colorScheme.onPrimaryContainer,
                        )
                        Text(
                            text = code,
                            fontSize = 40.sp,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 8.sp,
                            color = MaterialTheme.colorScheme.onPrimaryContainer,
                        )
                    } else {
                        Text(
                            text = "코드를 여러 번 잘못 입력해 연결을 막았습니다.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onPrimaryContainer,
                        )
                        Button(onClick = { sendIntent(SettingDataMigrationSendIntent.OnClickRenewCode) }) {
                            Text("새 코드 받기")
                        }
                    }
                }
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
                description = "위의 6자리 코드를 iPhone에 입력합니다. 다 옮겨질 때까지 이 화면을 열어 두세요.",
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
private fun SettingDataMigrationSendScreenLockedPreview() {
    JellyTubeTheme {
        SettingDataMigrationSendScreen(
            uiState = SettingDataMigrationSendUiState(pairingCode = null),
            navigateToBack = {},
            sendIntent = {},
        )
    }
}
