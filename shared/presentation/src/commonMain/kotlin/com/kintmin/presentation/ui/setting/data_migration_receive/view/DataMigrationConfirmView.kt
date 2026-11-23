package com.kintmin.presentation.ui.setting.data_migration_receive.view

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Lock
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.kintmin.presentation.theme.JellyTubeButtonColors
import com.kintmin.presentation.theme.JellyTubeTheme
import com.kintmin.presentation.ui.common.DownloadPlaylistSelectorBottomSheet
import com.kintmin.presentation.ui.common.DownloadPlaylistUiState
import com.kintmin.presentation.ui.setting.data_migration_receive.DataMigrationConfirmPlaylistItem
import com.kintmin.presentation.ui.setting.data_migration_receive.DataMigrationReceivePhase
import com.kintmin.presentation.ui.setting.data_migration_receive.SettingDataMigrationReceiveIntent
import com.kintmin.presentation.ui.setting.section.SettingItemView
import com.kintmin.presentation.ui.setting.section.SettingNoticeCardView

/** 파일을 받기 전에 무엇이 들어오는지 보이고 받는 방식을 정한다. */
@Composable
fun DataMigrationConfirmView(
    phase: DataMigrationReceivePhase.Confirming,
    sendIntent: (SettingDataMigrationReceiveIntent) -> Unit,
) {
    var isDestinationSheetVisible by remember { mutableStateOf(false) }

    Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer),
            shape = MaterialTheme.shapes.medium,
        ) {
            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("이것을 받습니다", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                Text(
                    text = "새 음원 ${phase.newAudioMediaCount}곡 · 플레이리스트 ${phase.playlists.size}개",
                    style = MaterialTheme.typography.bodyMedium,
                )
                Text(
                    text = "이미 있는 곡은 이 기기의 정보를 그대로 둡니다.",
                    style = MaterialTheme.typography.bodySmall,
                )
            }
        }

        if (phase.secretPlaylistCount > 0) {
            SettingNoticeCardView(
                icon = Icons.Rounded.Lock,
                title = "비밀 플레이리스트 ${phase.secretPlaylistCount}개가 들어옵니다",
                text = "받은 뒤에는 비밀모드를 켜야 보입니다. 보내는 기기에서 비밀모드를 켜고 보냈습니다.",
            )
        }

        if (phase.conflictingPlaylists.isNotEmpty()) {
            MergeChoiceSection(
                conflictingPlaylists = phase.conflictingPlaylists,
                sendIntent = sendIntent,
            )
        }

        if (phase.destinationCandidates.isNotEmpty()) {
            SettingItemView(
                title = "받은 곡을 담을 플레이리스트",
                description = phase.destinationCandidates.firstOrNull { it.isSelected }?.name ?: "미분류",
                onClick = { isDestinationSheetVisible = true },
            )
        }

        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            OutlinedButton(
                onClick = { sendIntent(SettingDataMigrationReceiveIntent.OnDeclineReceive) },
                colors = JellyTubeButtonColors.secondary(),
            ) {
                Text("받지 않기")
            }
            Button(onClick = { sendIntent(SettingDataMigrationReceiveIntent.OnClickStartReceive) }) {
                Text("받기")
            }
        }
    }

    if (!isDestinationSheetVisible) return
    DownloadPlaylistSelectorBottomSheet(
        playlistList = phase.destinationCandidates,
        onSelectPlaylist = { sendIntent(SettingDataMigrationReceiveIntent.OnSelectDestinationPlaylist(it)) },
        onDismissRequest = { isDestinationSheetVisible = false },
    )
}

@Composable
private fun MergeChoiceSection(
    conflictingPlaylists: List<DataMigrationConfirmPlaylistItem>,
    sendIntent: (SettingDataMigrationReceiveIntent) -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Text(
            text = "이름이 겹치는 플레이리스트 ${conflictingPlaylists.size}개",
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.Bold,
        )
        Text(
            text = "고르지 않은 것은 같은 이름으로 따로 만듭니다.",
            style = MaterialTheme.typography.bodySmall,
        )
        Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
            TextButton(onClick = { sendIntent(SettingDataMigrationReceiveIntent.OnClickMergeAll(true)) }) {
                Text("전부 합치기")
            }
            TextButton(onClick = { sendIntent(SettingDataMigrationReceiveIntent.OnClickMergeAll(false)) }) {
                Text("전부 따로 만들기")
            }
        }
        conflictingPlaylists.forEach { playlist ->
            DataMigrationConfirmPlaylistRowView(
                playlist = playlist,
                onClick = {
                    sendIntent(SettingDataMigrationReceiveIntent.OnToggleMergePlaylist(playlist.sourceId))
                },
            )
        }
    }
}

@Preview
@Composable
private fun DataMigrationConfirmViewPreview() {
    JellyTubeTheme {
        DataMigrationConfirmView(
            phase = DataMigrationReceivePhase.Confirming(
                newAudioMediaCount = 12,
                playlists = listOf(
                    DataMigrationConfirmPlaylistItem(1, "운동할 때", 8, isSecret = false, isConflicting = true, isMerging = false),
                    DataMigrationConfirmPlaylistItem(2, "공유받은 즐겨찾기", 4, isSecret = false, isConflicting = false, isMerging = false),
                ),
                secretPlaylistCount = 1,
                destinationCandidates = listOf(DownloadPlaylistUiState(1, "미분류", isSelected = true)),
            ),
            sendIntent = {},
        )
    }
}
