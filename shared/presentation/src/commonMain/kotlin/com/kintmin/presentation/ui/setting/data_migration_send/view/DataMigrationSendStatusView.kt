package com.kintmin.presentation.ui.setting.data_migration_send.view

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.kintmin.presentation.theme.JellyTubeTheme
import com.kintmin.presentation.ui.setting.data_migration_send.DataMigrationSendStep
import com.kintmin.presentation.ui.setting.data_migration_send.SettingDataMigrationSendIntent

@Composable
fun DataMigrationSendStatusView(
    step: DataMigrationSendStep,
    sendIntent: (SettingDataMigrationSendIntent) -> Unit,
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
            when (step) {
                DataMigrationSendStep.Starting -> {
                    CircularProgressIndicator(modifier = Modifier.size(32.dp))
                }
                is DataMigrationSendStep.WaitingForPair -> {
                    StatusText("연결 코드")
                    Text(
                        text = step.code,
                        fontSize = 40.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 8.sp,
                        color = MaterialTheme.colorScheme.onPrimaryContainer,
                    )
                    StatusText("iPhone에 이 코드를 입력해 주세요.")
                }
                DataMigrationSendStep.Sending -> {
                    CircularProgressIndicator(modifier = Modifier.size(32.dp))
                    StatusText("iPhone이 데이터를 받고 있습니다. 처음에는 보내기 전에 데이터를 점검합니다.")
                }
                is DataMigrationSendStep.Finished -> {
                    StatusText("iPhone이 받기를 마쳤습니다.")
                    StatusText("새 음원 ${step.newAudioMediaCount}곡을 옮겼습니다.")
                    if (step.failedCount > 0) {
                        StatusText(
                            "옮기지 못한 곡이 ${step.failedCount}곡 있습니다. iPhone에서 다시 시도하려면 이 화면을 열어 두세요.",
                        )
                    }
                }
                DataMigrationSendStep.Locked -> {
                    StatusText("코드를 여러 번 잘못 입력해 연결을 막았습니다.")
                    Button(onClick = { sendIntent(SettingDataMigrationSendIntent.OnClickRenewCode) }) {
                        Text("새 코드 받기")
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
private fun DataMigrationSendStatusViewWaitingPreview() {
    JellyTubeTheme {
        DataMigrationSendStatusView(step = DataMigrationSendStep.WaitingForPair("482913"), sendIntent = {})
    }
}

@Preview
@Composable
private fun DataMigrationSendStatusViewFinishedPreview() {
    JellyTubeTheme {
        DataMigrationSendStatusView(step = DataMigrationSendStep.Finished(120, 3), sendIntent = {})
    }
}
