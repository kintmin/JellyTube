package com.kintmin.presentation.ui.setting.backup_restore.view

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.kintmin.presentation.theme.JellyTubeButtonColors
import com.kintmin.presentation.theme.JellyTubeTheme
import com.kintmin.presentation.ui.common.SectionHeader
import com.kintmin.presentation.ui.setting.backup_restore.SettingBackupRestoreUiState

/** 백업은 매일 저절로 돌아 이 구역의 버튼은 보조 동작이다. 화면의 주 동작은 복원 쪽 하나다. */
@Composable
fun BackupSectionView(
    uiState: SettingBackupRestoreUiState,
    onClickBackupNow: () -> Unit,
    onClickPickFolder: () -> Unit,
) {
    val isBusy = uiState.isBackingUp || uiState.isRestoring
    Column {
        SectionHeader(text = "백업")
        Column(
            modifier = Modifier.padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            Text(
                text = uiState.folderLabel?.let { "위치: $it" } ?: "백업할 폴더를 아직 고르지 않았습니다",
                style = MaterialTheme.typography.bodyLarge,
            )
            Text(
                text = uiState.backupDescription,
                style = MaterialTheme.typography.bodyMedium,
                color = if (uiState.isBackupFailing) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Text(
                text = "하루 한 번 곡 목록·재생목록·가사·걸음수를 저장합니다. 음원 파일은 싣지 않습니다.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Row(
                modifier = Modifier.padding(top = 6.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                OutlinedButton(
                    onClick = onClickBackupNow,
                    enabled = uiState.folderLabel != null && !isBusy,
                    colors = JellyTubeButtonColors.secondary(),
                ) {
                    if (uiState.isBackingUp) {
                        CircularProgressIndicator(modifier = Modifier.size(18.dp), strokeWidth = 2.dp)
                    } else {
                        Text("지금 백업")
                    }
                }
                OutlinedButton(onClick = onClickPickFolder, enabled = !isBusy, colors = JellyTubeButtonColors.secondary()) {
                    Text(if (uiState.folderLabel == null) "폴더 고르기" else "폴더 바꾸기")
                }
            }
        }
    }
}

@Preview
@Composable
private fun BackupSectionViewPreview() {
    JellyTubeTheme {
        Surface {
            BackupSectionView(uiState = SettingBackupRestoreUiState.getMock(), onClickBackupNow = {}, onClickPickFolder = {})
        }
    }
}

@Preview
@Composable
private fun BackupSectionViewPickedFolderPreview() {
    JellyTubeTheme {
        Surface {
            BackupSectionView(
                uiState = SettingBackupRestoreUiState.getMock().copy(
                    folderLabel = "iCloud Drive/JellyTube",
                    backupDescription = "백업 폴더에 접근할 수 없습니다. 폴더를 다시 지정하세요",
                    isBackupFailing = true,
                ),
                onClickBackupNow = {},
                onClickPickFolder = {},
            )
        }
    }
}
