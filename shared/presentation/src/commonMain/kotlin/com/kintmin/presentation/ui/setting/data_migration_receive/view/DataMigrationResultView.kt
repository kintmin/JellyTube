package com.kintmin.presentation.ui.setting.data_migration_receive.view

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.kintmin.presentation.theme.JellyTubeTheme
import com.kintmin.presentation.ui.setting.data_migration_receive.DataMigrationFailureItem
import com.kintmin.presentation.ui.setting.data_migration_receive.DataMigrationReceivePhase

/** 끝났거나 멈춘 뒤의 결과와 다음 행동. */
@Composable
fun DataMigrationResultView(
    phase: DataMigrationReceivePhase,
    onClickRetry: () -> Unit,
    onClickExit: () -> Unit,
) {
    val isFailure = phase is DataMigrationReceivePhase.Interrupted ||
        (phase is DataMigrationReceivePhase.Finished && phase.failures.isNotEmpty())
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = if (isFailure) MaterialTheme.colorScheme.errorContainer else MaterialTheme.colorScheme.primaryContainer,
        ),
        shape = RoundedCornerShape(12.dp),
    ) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            when (phase) {
                is DataMigrationReceivePhase.Interrupted -> {
                    Text("가져오기가 멈췄습니다", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                    Text(phase.message, style = MaterialTheme.typography.bodySmall)
                }
                is DataMigrationReceivePhase.Finished -> {
                    val title = if (phase.failures.isEmpty()) "모두 가져왔습니다" else "일부 곡을 가져오지 못했습니다"
                    Text(title, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                    Text(
                        text = "새 음원 ${phase.newAudioMediaCount}곡 · 새 재생목록 ${phase.newPlaylistCount}개 · 실패 ${phase.failures.size}곡",
                        style = MaterialTheme.typography.bodyMedium,
                    )
                    if (phase.redownloadedCount > 0) {
                        Text(
                            text = "iPhone에서 재생할 수 없는 형식 ${phase.redownloadedCount}곡은 원본에서 다시 받았습니다.",
                            style = MaterialTheme.typography.bodySmall,
                        )
                    }
                    if (phase.failures.isNotEmpty()) {
                        HorizontalDivider()
                        phase.failures.forEach { FailureRow(it) }
                    }
                }
                else -> Unit
            }
            if (isFailure) {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedButton(onClick = onClickExit) { Text("나가기") }
                    Button(onClick = onClickRetry) { Text("다시 시도") }
                }
            }
        }
    }
}

@Composable
private fun FailureRow(failure: DataMigrationFailureItem) {
    Column {
        Text(failure.audioMediaName, style = MaterialTheme.typography.bodyMedium)
        Text(failure.reason, style = MaterialTheme.typography.labelSmall)
    }
}

@Preview
@Composable
private fun DataMigrationResultViewFailuresPreview() {
    JellyTubeTheme {
        DataMigrationResultView(
            phase = DataMigrationReceivePhase.Finished(
                newAudioMediaCount = 27,
                newPlaylistCount = 2,
                redownloadedCount = 1,
                failures = listOf(
                    DataMigrationFailureItem("녹음 파일", "iPhone에서 재생할 수 없는 형식"),
                    DataMigrationFailureItem("라이브 클립", "원본에서 다시 받을 수 없음"),
                    DataMigrationFailureItem("커버곡", "전송 실패"),
                ),
            ),
            onClickRetry = {},
            onClickExit = {},
        )
    }
}

@Preview
@Composable
private fun DataMigrationResultViewSuccessPreview() {
    JellyTubeTheme {
        DataMigrationResultView(
            phase = DataMigrationReceivePhase.Finished(30, 2, 0, emptyList()),
            onClickRetry = {},
            onClickExit = {},
        )
    }
}
