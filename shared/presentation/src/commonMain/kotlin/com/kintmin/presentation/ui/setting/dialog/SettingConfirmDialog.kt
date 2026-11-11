package com.kintmin.presentation.ui.setting.dialog

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.kintmin.presentation.theme.JellyTubeTheme
import com.kintmin.presentation.ui.common.JellyTubeDialog

@Composable
fun SettingConfirmDialog(
    isShow: Boolean,
    title: String,
    message: String,
    dismissText: String,
    confirmText: String,
    onDismiss: () -> Unit,
    onConfirm: () -> Unit,
) {
    JellyTubeDialog(
        showDialog = isShow,
        onDismiss = onDismiss,
    ) {
        Column(
            modifier = Modifier.padding(vertical = 12.dp, horizontal = 24.dp),
        ) {
            Text(
                modifier = Modifier.padding(bottom = 8.dp),
                text = title,
                style = MaterialTheme.typography.titleLarge,
            )
            Text(
                modifier = Modifier.padding(bottom = 16.dp),
                text = message,
                style = MaterialTheme.typography.bodyMedium,
            )
            Row(
                modifier = Modifier.align(Alignment.End),
            ) {
                OutlinedButton(
                    modifier = Modifier.padding(end = 8.dp),
                    contentPadding = PaddingValues(horizontal = 24.dp, vertical = 0.dp),
                    onClick = onDismiss,
                ) {
                    Text(text = dismissText, fontSize = 14.sp)
                }
                Button(
                    contentPadding = PaddingValues(horizontal = 24.dp, vertical = 0.dp),
                    onClick = {
                        onConfirm()
                        onDismiss()
                    },
                ) {
                    Text(text = confirmText, fontSize = 14.sp)
                }
            }
        }
    }
}

@Preview
@Composable
private fun SettingConfirmDialogPreview() {
    JellyTubeTheme {
        SettingConfirmDialog(
            isShow = true,
            title = "보내기를 중단할까요?",
            message = "iPhone이 아직 받는 중입니다. 중단하면 iPhone에서 실패로 끝납니다.",
            dismissText = "계속 보내기",
            confirmText = "중단",
            onDismiss = {},
            onConfirm = {},
        )
    }
}
