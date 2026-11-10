package com.kintmin.presentation.ui.audio_edit.dialog

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
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
import com.kintmin.presentation.theme.JellyTubeTheme
import com.kintmin.presentation.ui.common.JellyTubeDialog

@Composable
fun AudioEditExitDialog(
    isShow: Boolean,
    onDismiss: () -> Unit,
    onConfirmExit: () -> Unit,
) {
    JellyTubeDialog(
        showDialog = isShow,
        onDismiss = onDismiss,
    ) {
        Column(
            modifier = Modifier.padding(vertical = 12.dp, horizontal = 24.dp),
        ) {
            Text(
                modifier = Modifier.padding(vertical = 12.dp),
                text = "편집을 취소할까요?",
                style = MaterialTheme.typography.titleLarge,
            )
            Text(
                text = "저장하지 않은 편집 내용이 사라집니다.",
                style = MaterialTheme.typography.bodyMedium,
            )

            Row(
                modifier = Modifier
                    .align(Alignment.End)
                    .padding(top = 20.dp, bottom = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                OutlinedButton(onClick = onDismiss) {
                    Text(text = "계속 편집")
                }
                Button(
                    onClick = {
                        onConfirmExit()
                        onDismiss()
                    },
                ) {
                    Text(text = "나가기")
                }
            }
        }
    }
}

@Preview
@Composable
private fun AudioEditExitDialogPreview() {
    JellyTubeTheme {
        AudioEditExitDialog(
            isShow = true,
            onDismiss = {},
            onConfirmExit = {},
        )
    }
}
