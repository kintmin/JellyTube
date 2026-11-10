package com.kintmin.presentation.ui.setting.dialog

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.kintmin.presentation.theme.JellyTubeTheme
import com.kintmin.presentation.ui.common.JellyTubeDialog

/**
 * 걸음수 권한이 거부·제한됐을 때. 한 번 거부되면 OS 가 권한 창을 다시 띄워 주지 않아
 * 설정으로 보내는 것이 유일한 길이다.
 */
@Composable
fun MotionPermissionDialog(
    showDialog: Boolean,
    onOpenSettings: () -> Unit,
    onDismiss: () -> Unit,
) {
    JellyTubeDialog(showDialog = showDialog, onDismiss = onDismiss) {
        Column(modifier = Modifier.padding(24.dp)) {
            Text(
                text = "걸음수를 세려면 권한이 필요합니다",
                style = MaterialTheme.typography.titleMedium,
            )
            Text(
                modifier = Modifier.padding(top = 12.dp),
                text = "설정에서 동작 및 피트니스(신체 활동)를 허용해주세요.\n\n" +
                    "앱 설정에 그 항목이 없다면 기기 전체에서 꺼져 있는 것입니다.\n" +
                    "설정 > 개인정보 보호 및 보안 > 동작 및 피트니스 > 피트니스 추적을 먼저 켜주세요.",
                style = MaterialTheme.typography.bodyMedium,
            )

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 20.dp),
                horizontalArrangement = Arrangement.End,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                TextButton(onClick = onDismiss) { Text("닫기") }
                TextButton(onClick = onOpenSettings) { Text("설정 열기") }
            }
        }
    }
}

@Preview
@Composable
private fun MotionPermissionDialogPreview() {
    JellyTubeTheme {
        MotionPermissionDialog(showDialog = true, onOpenSettings = {}, onDismiss = {})
    }
}
