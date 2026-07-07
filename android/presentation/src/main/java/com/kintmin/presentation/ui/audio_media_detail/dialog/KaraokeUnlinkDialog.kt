package com.kintmin.presentation.ui.audio_media_detail.dialog

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
fun KaraokeUnlinkDialog(
    isShow: Boolean,
    onDismiss: () -> Unit,
    onConfirmUnlink: () -> Unit,
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
                text = "노래방 번호 연동 해제",
                style = MaterialTheme.typography.titleLarge,
            )
            Text(
                modifier = Modifier.padding(bottom = 16.dp),
                text = "연동된 TJ 미디어 노래방 번호를 연동 해제하시겠습니까?",
                style = MaterialTheme.typography.bodyMedium,
            )
            Row(
                modifier = Modifier.align(Alignment.End)
            ) {
                OutlinedButton(
                    modifier = Modifier.padding(end = 8.dp),
                    contentPadding = PaddingValues(horizontal = 24.dp, vertical = 0.dp),
                    onClick = { onDismiss() }) {
                    Text(
                        text = "취소",
                        fontSize = 14.sp,
                    )
                }
                Button(
                    contentPadding = PaddingValues(horizontal = 32.dp, vertical = 0.dp),
                    onClick = {
                        onConfirmUnlink()
                        onDismiss()
                    }) {
                    Text(
                        text = "해제",
                        fontSize = 14.sp,
                    )
                }
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun KaraokeUnlinkDialogPreview() {
    JellyTubeTheme {
        KaraokeUnlinkDialog(
            isShow = true,
            onDismiss = {},
            onConfirmUnlink = {},
        )
    }
}
