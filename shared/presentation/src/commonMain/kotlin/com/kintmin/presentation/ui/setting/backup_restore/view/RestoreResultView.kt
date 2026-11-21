package com.kintmin.presentation.ui.setting.backup_restore.view

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.material3.Button
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.kintmin.presentation.theme.JellyTubeButtonColors
import com.kintmin.presentation.theme.JellyTubeTheme
import com.kintmin.presentation.ui.setting.backup_restore.RestorePhase
import com.kintmin.presentation.ui.setting.data_migration_receive.DataMigrationFailureItem

/** 끝났거나 멈춘 뒤의 결과와 다음 행동. */
@Composable
fun RestoreResultView(
    phase: RestorePhase,
    onClickRetry: () -> Unit,
    onClickClose: () -> Unit,
) {
    val isFailure = phase is RestorePhase.Interrupted || (phase is RestorePhase.Finished && phase.failures.isNotEmpty())
    val titleColor = if (isFailure) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        when (phase) {
            is RestorePhase.Interrupted -> {
                Text("복원이 멈췄습니다", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold, color = titleColor)
                Text(phase.message, style = MaterialTheme.typography.bodySmall)
            }
            is RestorePhase.Finished -> {
                val title = if (phase.failures.isEmpty()) "모두 복원했습니다" else "일부 곡을 복원하지 못했습니다"
                Text(title, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold, color = titleColor)
                Text(
                    text = "새 음원 ${phase.newAudioMediaCount}곡 · 새 재생목록 ${phase.newPlaylistCount}개 · 실패 ${phase.failures.size}곡",
                    style = MaterialTheme.typography.bodyMedium,
                )
                if (phase.failures.isNotEmpty()) {
                    HorizontalDivider()
                    phase.failures.forEach { FailureRow(it) }
                    Text(
                        text = "같은 백업으로 다시 복원하면 실패한 곡만 다시 받습니다.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
            else -> Unit
        }
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            OutlinedButton(onClick = onClickClose, colors = JellyTubeButtonColors.secondary()) { Text(if (isFailure) "닫기" else "확인") }
            if (phase is RestorePhase.Interrupted) Button(onClick = onClickRetry) { Text("다시 시도") }
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
private fun RestoreResultViewFailuresPreview() {
    JellyTubeTheme {
        Surface {
            RestoreResultView(
                phase = RestorePhase.Finished(
                    newAudioMediaCount = 420,
                    newPlaylistCount = 6,
                    failures = listOf(
                        DataMigrationFailureItem("녹음 파일", "원본이 유튜브 링크가 아니라 다시 받을 수 없음"),
                        DataMigrationFailureItem("비공개된 영상", "원본에서 다시 받지 못함"),
                    ),
                ),
                onClickRetry = {},
                onClickClose = {},
            )
        }
    }
}

@Preview
@Composable
private fun RestoreResultViewInterruptedPreview() {
    JellyTubeTheme {
        Surface {
            RestoreResultView(
                phase = RestorePhase.Interrupted("복원이 멈췄습니다. 다시 시도하면 받아 둔 곡은 건너뛰고 빠진 곡만 받습니다."),
                onClickRetry = {},
                onClickClose = {},
            )
        }
    }
}
