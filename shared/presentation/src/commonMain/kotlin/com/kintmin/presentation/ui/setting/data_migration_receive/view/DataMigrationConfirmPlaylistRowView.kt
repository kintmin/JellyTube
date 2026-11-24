package com.kintmin.presentation.ui.setting.data_migration_receive.view

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Lock
import androidx.compose.material3.Checkbox
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.kintmin.presentation.theme.JellyTubeTheme
import com.kintmin.presentation.theme.contentMuted
import com.kintmin.presentation.ui.setting.data_migration_receive.DataMigrationConfirmPlaylistItem

/** 이름이 겹치는 플레이리스트 한 줄. 체크하면 이 기기의 같은 이름에 합친다. */
@Composable
fun DataMigrationConfirmPlaylistRowView(
    playlist: DataMigrationConfirmPlaylistItem,
    onClick: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        Checkbox(checked = playlist.isMerging, onCheckedChange = { onClick() })
        if (playlist.isSecret) {
            Icon(
                imageVector = Icons.Rounded.Lock,
                contentDescription = "비밀 플레이리스트",
                modifier = Modifier.size(14.dp),
                tint = contentMuted,
            )
        }
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = playlist.name,
                style = MaterialTheme.typography.bodyMedium,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                text = if (playlist.isMerging) {
                    "${playlist.audioMediaCount}곡을 이 플레이리스트에 더합니다"
                } else {
                    "${playlist.audioMediaCount}곡을 담은 플레이리스트를 따로 만듭니다"
                },
                style = MaterialTheme.typography.labelSmall,
                color = contentMuted,
            )
        }
    }
}

@Preview
@Composable
private fun DataMigrationConfirmPlaylistRowViewPreview() {
    JellyTubeTheme {
        Column {
            DataMigrationConfirmPlaylistRowView(
                playlist = DataMigrationConfirmPlaylistItem(1, "운동할 때", 8, false, isConflicting = true, isMerging = true),
                onClick = {},
            )
            DataMigrationConfirmPlaylistRowView(
                playlist = DataMigrationConfirmPlaylistItem(2, "나만 듣는 곡", 3, true, isConflicting = true, isMerging = false),
                onClick = {},
            )
        }
    }
}
