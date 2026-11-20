package com.kintmin.presentation.ui.lyrics_edit.dialog

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
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
fun LyricsEditExitDialog(
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
                modifier = Modifier.padding(bottom = 8.dp),
                text = "편집 나가기",
                style = MaterialTheme.typography.titleLarge,
            )
            Text(
                modifier = Modifier.padding(bottom = 16.dp),
                text = "저장하지 않은 편집 내용은 사라집니다.",
                style = MaterialTheme.typography.bodyMedium,
            )
            Row(
                modifier = Modifier.align(Alignment.End),
            ) {
                OutlinedButton(
                    // 파괴적 버튼과 간격을 벌려 반사적으로 눌리지 않게 한다.
                    modifier = Modifier.padding(end = 24.dp),
                    contentPadding = PaddingValues(horizontal = 24.dp, vertical = 0.dp),
                    colors = ButtonDefaults.outlinedButtonColors(
                        contentColor = MaterialTheme.colorScheme.onSurface,
                    ),
                    onClick = { onDismiss() },
                ) {
                    Text(
                        text = "계속 편집",
                    )
                }
                Button(
                    contentPadding = PaddingValues(horizontal = 32.dp, vertical = 0.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.error,
                        contentColor = MaterialTheme.colorScheme.onError,
                    ),
                    onClick = {
                        onConfirmExit()
                        onDismiss()
                    },
                ) {
                    Text(
                        text = "나가기",
                    )
                }
            }
        }
    }
}

@Preview
@Composable
private fun LyricsEditExitDialogPreview() {
    JellyTubeTheme {
        LyricsEditExitDialog(
            isShow = true,
            onDismiss = {},
            onConfirmExit = {},
        )
    }
}
