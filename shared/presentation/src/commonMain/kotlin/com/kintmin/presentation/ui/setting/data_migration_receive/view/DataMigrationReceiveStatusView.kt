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
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.kintmin.presentation.theme.JellyTubeTheme
import com.kintmin.presentation.ui.setting.data_migration_receive.DataMigrationReceiveStep
import com.kintmin.presentation.ui.setting.data_migration_receive.SettingDataMigrationReceiveIntent
import com.kintmin.presentation.ui.setting.data_migration_receive.SettingDataMigrationReceiveUiState

@Composable
fun DataMigrationReceiveStatusView(
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
            when (val step = uiState.step) {
                DataMigrationReceiveStep.Idle -> {
                    StatusText("같은 Wi-Fi의 Android 기기를 찾습니다.")
                    Button(onClick = { sendIntent(SettingDataMigrationReceiveIntent.OnClickSearch) }) {
                        Text("기기 찾기")
                    }
                }
                DataMigrationReceiveStep.Searching -> {
                    CircularProgressIndicator(modifier = Modifier.size(32.dp))
                    StatusText("Android 기기를 찾는 중입니다.")
                }
                DataMigrationReceiveStep.NotFound -> {
                    StatusText(
                        "기기를 찾지 못했습니다. Android에서 보내기 화면이 열려 있는지, 같은 Wi-Fi인지, " +
                            "설정 → 개인정보 보호 → 로컬 네트워크에서 JellyTube가 켜져 있는지 확인해 주세요.",
                    )
                    Button(onClick = { sendIntent(SettingDataMigrationReceiveIntent.OnClickSearch) }) {
                        Text("다시 찾기")
                    }
                }
                is DataMigrationReceiveStep.CodeInput -> {
                    StatusText("Android 화면에 보이는 6자리 코드를 입력해 주세요.")
                    OutlinedTextField(
                        value = uiState.code,
                        onValueChange = { sendIntent(SettingDataMigrationReceiveIntent.OnChangeCode(it)) },
                        singleLine = true,
                        isError = step.errorMessage != null,
                        supportingText = step.errorMessage?.let { message -> { Text(message) } },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
                    )
                    Button(
                        enabled = uiState.canConnect,
                        onClick = { sendIntent(SettingDataMigrationReceiveIntent.OnClickConnect) },
                    ) {
                        Text("연결")
                    }
                }
                DataMigrationReceiveStep.Pairing -> {
                    CircularProgressIndicator(modifier = Modifier.size(32.dp))
                    StatusText("연결하는 중입니다.")
                }
                is DataMigrationReceiveStep.Transferring -> {
                    LinearProgressIndicator(
                        progress = { step.fraction },
                        modifier = Modifier.fillMaxWidth(),
                    )
                    StatusText("${step.receivedBytes.toMegabytes()} / ${step.totalBytes.toMegabytes()} MB")
                    StatusText("다 옮겨질 때까지 이 화면을 열어 두세요. 화면은 자동으로 꺼지지 않습니다.")
                }
                is DataMigrationReceiveStep.Completed -> {
                    StatusText("옮기기를 마쳤습니다.")
                    StatusText("새 음원 ${step.newAudioMediaCount}곡, 새 재생목록 ${step.newPlaylistCount}개")
                }
                is DataMigrationReceiveStep.Failed -> {
                    StatusText(step.message)
                    Button(onClick = { sendIntent(SettingDataMigrationReceiveIntent.OnClickRetry) }) {
                        Text("다시 시도")
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
    )
}

private fun Long.toMegabytes(): Long = this / 1_000_000

@Preview
@Composable
private fun DataMigrationReceiveStatusViewTransferringPreview() {
    JellyTubeTheme {
        DataMigrationReceiveStatusView(
            uiState = SettingDataMigrationReceiveUiState.getMock(),
            sendIntent = {},
        )
    }
}

@Preview
@Composable
private fun DataMigrationReceiveStatusViewCodeInputPreview() {
    JellyTubeTheme {
        DataMigrationReceiveStatusView(
            uiState = SettingDataMigrationReceiveUiState(
                step = DataMigrationReceiveStep.CodeInput(errorMessage = "코드가 맞지 않습니다."),
                code = "4829",
            ),
            sendIntent = {},
        )
    }
}
