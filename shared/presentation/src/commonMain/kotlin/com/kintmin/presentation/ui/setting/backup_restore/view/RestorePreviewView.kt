package com.kintmin.presentation.ui.setting.backup_restore.view

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.Button
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
import com.kintmin.presentation.ui.setting.backup_restore.RestorePreviewItem

@Composable
fun RestorePreviewView(
    preview: RestorePreviewItem,
    onClickStart: () -> Unit,
    /** null 이면 그만둘 것이 없어 취소 버튼을 두지 않는다. */
    onClickCancel: (() -> Unit)?,
) {
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Text(text = "${preview.createdAt} 백업", style = MaterialTheme.typography.bodyLarge)
        CountLine("백업에 있는 곡", "${preview.totalCount}곡")
        CountLine("이미 이 기기에 있음", "${preview.alreadyOnDeviceCount}곡")
        if (preview.redownloadCount > 0) {
            CountLine("원본에서 다시 받음", "${preview.redownloadCount}곡 · 약 ${preview.estimatedMegabytes} MB")
        } else {
            Text(
                text = "백업의 곡이 이 기기에 모두 있습니다. 복원하면 빠진 재생목록 소속과 걸음수만 맞춥니다.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        if (preview.notRestorableCount > 0) {
            CountLine("다시 받을 수 없음", "${preview.notRestorableCount}곡")
            Text(
                text = "직접 가져온 파일처럼 원본이 유튜브 링크가 아닌 곡은 복원할 수 없습니다.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        Text(
            text = "이미 있는 곡은 그대로 두고, 같은 이름의 재생목록은 하나로 합칩니다. " +
                "편집한 음원과 직접 바꾼 재생목록 표지는 원본으로 돌아옵니다.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            onClickCancel?.let { OutlinedButton(onClick = it, colors = JellyTubeButtonColors.secondary()) { Text("취소") } }
            Button(onClick = onClickStart) { Text("복원하기") }
        }
    }
}

@Composable
private fun CountLine(label: String, value: String) {
    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
        Text(label, style = MaterialTheme.typography.bodyMedium)
        Text(value, style = MaterialTheme.typography.bodyMedium)
    }
}

@Preview
@Composable
private fun RestorePreviewViewPreview() {
    JellyTubeTheme {
        Surface {
            RestorePreviewView(
                preview = RestorePreviewItem("2026년 11월 20일 21시 4분", 452, 12, 431, 9, 1_650),
                onClickStart = {},
                onClickCancel = {},
            )
        }
    }
}
