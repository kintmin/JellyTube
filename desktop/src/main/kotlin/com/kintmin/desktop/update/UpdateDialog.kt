package com.kintmin.desktop.update

import androidx.compose.foundation.layout.Row
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import org.jetbrains.compose.ui.tooling.preview.Preview

/** MD3 다이얼로그처럼 버튼을 끝으로 모으고, 강조가 가장 낮은 "이번 버전 무시" 를 앞에 둔다. */
@Composable
fun UpdateDialog(
    versionName: String,
    onDownload: () -> Unit,
    onIgnore: () -> Unit,
    onDismiss: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("새 버전이 있습니다") },
        text = {
            Text(
                text = "JellyTube 파일 공유 $versionName 이 나왔습니다.\n받은 설치 파일을 열어 업데이트해 주세요.",
                style = MaterialTheme.typography.bodyMedium,
            )
        },
        confirmButton = {
            Button(onClick = onDownload) { Text("다운로드") }
        },
        dismissButton = {
            Row {
                TextButton(onClick = onIgnore) { Text("이번 버전 무시") }
                TextButton(onClick = onDismiss) { Text("닫기") }
            }
        },
    )
}

@Preview
@Composable
private fun UpdateDialogPreview() {
    MaterialTheme {
        UpdateDialog(versionName = "1.1.0", onDownload = {}, onIgnore = {}, onDismiss = {})
    }
}
