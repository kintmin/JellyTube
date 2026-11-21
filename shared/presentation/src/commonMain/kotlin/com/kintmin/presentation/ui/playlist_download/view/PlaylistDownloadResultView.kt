package com.kintmin.presentation.ui.playlist_download.view

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.kintmin.presentation.theme.JellyTubeButtonColors
import com.kintmin.presentation.theme.JellyTubeTheme
import com.kintmin.presentation.ui.playlist_download.PlaylistDownloadPhase
import com.kintmin.presentation.ui.setting.data_migration_receive.DataMigrationFailureItem

/** 끝났거나 멈춘 뒤의 결과. 주 행동은 화면 아래에 있고, 실패한 곡 다시 받기만 여기 둔다. */
@Composable
fun PlaylistDownloadResultView(
    phase: PlaylistDownloadPhase,
    onClickRetry: () -> Unit,
) {
    val isFailure = phase is PlaylistDownloadPhase.Interrupted ||
        (phase is PlaylistDownloadPhase.Finished && phase.failures.isNotEmpty())
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = if (isFailure) MaterialTheme.colorScheme.errorContainer else MaterialTheme.colorScheme.primaryContainer,
        ),
        shape = MaterialTheme.shapes.medium,
    ) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            when (phase) {
                is PlaylistDownloadPhase.Interrupted -> {
                    Text("받기가 멈췄습니다", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                    Text(phase.message, style = MaterialTheme.typography.bodySmall)
                }
                is PlaylistDownloadPhase.Finished -> {
                    val title = if (phase.failures.isEmpty()) "'${phase.playlistName}' 에 모두 담았습니다" else "일부 곡을 받지 못했습니다"
                    Text(title, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                    Text(
                        text = "새로 받음 ${phase.downloadedCount}곡 · 이미 있음 ${phase.alreadyOnDeviceCount}곡 · 실패 ${phase.failures.size}곡",
                        style = MaterialTheme.typography.bodyMedium,
                    )
                    if (phase.failures.isNotEmpty()) {
                        // 기본 구분선 색은 색이 깔린 카드 위에서 떠 보여 카드 글자색을 옅게 쓴다.
                        HorizontalDivider(color = LocalContentColor.current.copy(alpha = 0.3f))
                        phase.failures.forEach { FailureRow(it) }
                    }
                    if (phase.hasRetryableFailures) {
                        OutlinedButton(onClick = onClickRetry, colors = JellyTubeButtonColors.secondary()) { Text("실패한 곡 다시 받기") }
                    }
                }
                else -> Unit
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
private fun PlaylistDownloadResultViewFailuresPreview() {
    JellyTubeTheme {
        Surface {
            PlaylistDownloadResultView(
                phase = PlaylistDownloadPhase.Finished(
                    playlistId = 3,
                    playlistName = "드라이브할 때",
                    downloadedCount = 39,
                    alreadyOnDeviceCount = 5,
                    failures = listOf(
                        DataMigrationFailureItem("[Private video]", "비공개이거나 삭제된 영상"),
                        DataMigrationFailureItem("밤편지", "받지 못함"),
                    ),
                    hasRetryableFailures = true,
                ),
                onClickRetry = {},
            )
        }
    }
}

@Preview
@Composable
private fun PlaylistDownloadResultViewInterruptedPreview() {
    JellyTubeTheme {
        Surface {
            PlaylistDownloadResultView(
                phase = PlaylistDownloadPhase.Interrupted("받은 곡은 '드라이브할 때' 재생목록에 담았습니다. 다시 받으면 빠진 곡만 받아 제자리에 끼웁니다."),
                onClickRetry = {},
            )
        }
    }
}
