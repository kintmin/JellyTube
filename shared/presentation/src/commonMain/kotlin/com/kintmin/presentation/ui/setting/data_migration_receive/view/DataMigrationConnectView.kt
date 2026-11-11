package com.kintmin.presentation.ui.setting.data_migration_receive.view

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.kintmin.presentation.theme.JellyTubeTheme
import com.kintmin.presentation.ui.setting.data_migration_receive.DataMigrationReceivePhase
import com.kintmin.presentation.ui.setting.data_migration_receive.SettingDataMigrationReceiveIntent
import com.kintmin.presentation.ui.setting.data_migration_receive.SettingDataMigrationReceiveUiState

/** 연결 전 단계(탐색 → 코드 입력 → 연결)를 그린다. */
@Composable
fun DataMigrationConnectView(
    uiState: SettingDataMigrationReceiveUiState,
    sendIntent: (SettingDataMigrationReceiveIntent) -> Unit,
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
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            when (val phase = uiState.phase) {
                DataMigrationReceivePhase.Searching -> {
                    CircularProgressIndicator(modifier = Modifier.size(32.dp))
                    StatusText("Android 기기를 찾는 중입니다.")
                }
                DataMigrationReceivePhase.NotFound -> {
                    StatusText(
                        "기기를 찾지 못했습니다. Android에서 보내기 화면이 열려 있는지, 같은 Wi-Fi인지, " +
                            "설정 → 개인정보 보호 → 로컬 네트워크에서 JellyTube가 켜져 있는지 확인해 주세요.",
                    )
                    Button(onClick = { sendIntent(SettingDataMigrationReceiveIntent.OnClickSearch) }) {
                        Text("다시 찾기")
                    }
                }
                is DataMigrationReceivePhase.CodeInput -> {
                    StatusText("Android 화면에 보이는 6자리 코드를 입력해 주세요.")
                    OutlinedTextField(
                        value = uiState.code,
                        onValueChange = { sendIntent(SettingDataMigrationReceiveIntent.OnChangeCode(it)) },
                        singleLine = true,
                        isError = phase.errorMessage != null,
                        supportingText = phase.errorMessage?.let { message -> { Text(message) } },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
                    )
                    Button(
                        enabled = uiState.canConnect,
                        onClick = { sendIntent(SettingDataMigrationReceiveIntent.OnClickConnect) },
                    ) {
                        Text("연결하고 가져오기")
                    }
                }
                DataMigrationReceivePhase.Pairing -> {
                    CircularProgressIndicator(modifier = Modifier.size(32.dp))
                    StatusText("연결하는 중입니다.")
                }
                else -> {
                    StatusText("같은 Wi-Fi의 Android 기기를 찾습니다.")
                    Button(onClick = { sendIntent(SettingDataMigrationReceiveIntent.OnClickSearch) }) {
                        Text("기기 찾기")
                    }
                }
            }
        }
    }
}

@Composable
private fun StatusText(text: String) {
    Text(
        text = text,
        style = MaterialTheme.typography.bodyMedium,
        color = MaterialTheme.colorScheme.onPrimaryContainer,
        textAlign = TextAlign.Center,
    )
}

@Preview
@Composable
private fun DataMigrationConnectViewCodeInputPreview() {
    JellyTubeTheme {
        DataMigrationConnectView(
            uiState = SettingDataMigrationReceiveUiState(
                phase = DataMigrationReceivePhase.CodeInput(errorMessage = "코드가 맞지 않습니다."),
                code = "4829",
            ),
            sendIntent = {},
        )
    }
}
