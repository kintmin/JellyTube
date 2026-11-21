package com.kintmin.presentation.ui.playlist_download.view

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Card
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.kintmin.presentation.theme.JellyTubeTheme
import com.kintmin.presentation.ui.playlist_download.PlaylistDownloadPreviewItem

@Composable
fun PlaylistDownloadPreviewView(preview: PlaylistDownloadPreviewItem) {
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Card(modifier = Modifier.fillMaxWidth(), shape = MaterialTheme.shapes.medium) {
            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(text = preview.title, style = MaterialTheme.typography.titleMedium)
                HorizontalDivider()
                CountLine("재생목록의 곡", "${preview.totalCount}곡")
                CountLine("이미 이 기기에 있음", "${preview.alreadyOnDeviceCount}곡")
                if (preview.downloadCount > 0) {
                    CountLine("새로 받음", "${preview.downloadCount}곡 · 약 ${preview.estimatedMegabytes} MB")
                }
                if (preview.unavailableCount > 0) {
                    CountLine("받을 수 없음", "${preview.unavailableCount}곡")
                }
            }
        }
        Hint(
            if (preview.mergesIntoExistingPlaylist) {
                "같은 이름의 재생목록 '${preview.title}' 에 이어 담습니다. 이미 담긴 곡은 그대로 둡니다."
            } else {
                "새 재생목록 '${preview.title}' 을 만들어 유튜브 순서대로 담습니다."
            },
        )
        if (preview.unavailableCount > 0) Hint("비공개이거나 삭제된 영상은 받을 수 없습니다.")
        if (preview.isLarge) Hint("곡이 많아 오래 걸립니다. 멈춰도 받은 곡은 남아, 다시 받으면 이어 받습니다.")
    }
}

@Composable
private fun CountLine(label: String, value: String) {
    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
        Text(label, style = MaterialTheme.typography.bodyMedium)
        Text(value, style = MaterialTheme.typography.bodyMedium)
    }
}

@Composable
private fun Hint(text: String) {
    Text(text = text, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
}

@Preview
@Composable
private fun PlaylistDownloadPreviewViewPreview() {
    JellyTubeTheme {
        Surface {
            PlaylistDownloadPreviewView(preview = PlaylistDownloadPreviewItem("드라이브할 때", 248, 5, 241, 2, 960, true))
        }
    }
}
