package com.kintmin.presentation.ui.setting.backup_restore.view

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.kintmin.presentation.port.model.RunningTask
import com.kintmin.presentation.theme.JellyTubeButtonColors
import com.kintmin.presentation.theme.JellyTubeTheme
import com.kintmin.presentation.ui.common.SectionHeader
import com.kintmin.presentation.ui.setting.backup_restore.RestorePhase
import com.kintmin.presentation.ui.setting.backup_restore.RestorePreviewItem
import com.kintmin.presentation.ui.setting.backup_restore.SettingBackupRestoreUiState
import com.kintmin.presentation.ui.setting.data_migration_receive.view.DataMigrationStageRows

/** 복원의 모든 단계가 이 구역 안에서만 바뀐다. 백업 구역은 그대로 남는다. */
@Composable
fun RestoreSectionView(
    uiState: SettingBackupRestoreUiState,
    onClickFindBackupFile: () -> Unit,
    onClickPickFolder: () -> Unit,
    onClickStart: () -> Unit,
    onClickCancelPreview: () -> Unit,
    onClickStop: () -> Unit,
    onClickRetry: () -> Unit,
    onClickCloseResult: () -> Unit,
) {
    Column {
        SectionHeader(text = "복원", showDivider = true)
        Column(
            modifier = Modifier.padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            when (val phase = uiState.restore) {
                RestorePhase.Finding, RestorePhase.Reading -> CircularProgressIndicator(modifier = Modifier.size(32.dp))
                RestorePhase.NotFound, is RestorePhase.ReadFailed -> {
                    (phase as? RestorePhase.ReadFailed)?.let {
                        Text(it.message, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.error)
                    }
                    NotFoundGuide(uiState, onClickFindBackupFile, onClickPickFolder)
                }
                is RestorePhase.Blocked -> {
                    Text("아직 끝나지 않은 작업이 있습니다. 모두 끝난 뒤에 복원할 수 있습니다.", style = MaterialTheme.typography.bodyMedium)
                    phase.runningTasks.forEach { (task, count) ->
                        Text("· ${task.label} ${count}건", style = MaterialTheme.typography.bodyMedium)
                    }
                    Button(onClick = onClickRetry) { Text("다시 확인") }
                }
                is RestorePhase.Preview -> RestorePreviewView(
                    preview = phase.preview,
                    onClickStart = onClickStart,
                    onClickCancel = onClickCancelPreview.takeIf { phase.isPickedFile },
                )
                RestorePhase.Running -> {
                    DataMigrationStageRows(stages = uiState.stages, label = SettingBackupRestoreUiState::restoreStageLabel)
                    Text(
                        text = "멈춰도 받아 둔 곡은 남아, 다시 복원하면 이어 받습니다.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    OutlinedButton(onClick = onClickStop, colors = JellyTubeButtonColors.secondary()) { Text("멈추기") }
                }
                is RestorePhase.Interrupted, is RestorePhase.Finished -> {
                    DataMigrationStageRows(stages = uiState.stages, label = SettingBackupRestoreUiState::restoreStageLabel)
                    RestoreResultView(phase, onClickRetry, onClickCloseResult)
                }
            }
        }
    }
}

@Composable
private fun NotFoundGuide(
    uiState: SettingBackupRestoreUiState,
    onClickFindBackupFile: () -> Unit,
    onClickPickFolder: () -> Unit,
) {
    if (uiState.isFolderFixed) {
        Text(
            text = "이 앱에서 볼 수 있는 백업이 없습니다. 앱을 다시 설치했다면 ${uiState.folderLabel.orEmpty()} 의 " +
                "jellytube-backup.json.gz 를 직접 찾아 주세요.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        OutlinedButton(onClick = onClickFindBackupFile, colors = JellyTubeButtonColors.secondary()) { Text("백업 파일 찾기") }
    } else {
        Text(
            text = "백업이 없습니다. 앱을 다시 설치했다면 백업을 저장하던 폴더를 다시 골라 주세요.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        OutlinedButton(onClick = onClickPickFolder, colors = JellyTubeButtonColors.secondary()) { Text("백업 폴더 고르기") }
    }
}

@Composable
private fun PreviewSection(uiState: SettingBackupRestoreUiState) {
    JellyTubeTheme {
        Surface {
            RestoreSectionView(uiState, {}, {}, {}, {}, {}, {}, {})
        }
    }
}

@Preview
@Composable
private fun RestoreSectionViewReadyPreview() {
    PreviewSection(SettingBackupRestoreUiState.getMock())
}

@Preview
@Composable
private fun RestoreSectionViewNotFoundPreview() {
    PreviewSection(SettingBackupRestoreUiState.getMock().copy(restore = RestorePhase.NotFound))
}

@Preview
@Composable
private fun RestoreSectionViewPreviewPhasePreview() {
    PreviewSection(
        SettingBackupRestoreUiState.getMock().copy(
            restore = RestorePhase.Preview(
                RestorePreviewItem("2026년 11월 20일 21시 4분", 452, 12, 431, 9, 1_650),
                isPickedFile = true,
            ),
        ),
    )
}

@Preview
@Composable
private fun RestoreSectionViewRunningPreview() {
    PreviewSection(SettingBackupRestoreUiState.getRunningMock())
}

@Preview
@Composable
private fun RestoreSectionViewBlockedPreview() {
    PreviewSection(SettingBackupRestoreUiState.getMock().copy(restore = RestorePhase.Blocked(mapOf(RunningTask.YOUTUBE_DOWNLOAD to 2))))
}
