package com.kintmin.presentation.ui.audio_media_information_edit.dialog

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.ButtonDefaults
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
import com.kintmin.presentation.theme.softRed
import com.kintmin.presentation.ui.common.JellyTubeDialog

/** 되돌릴 수 있는 쪽이라 테두리 버튼에 빨간 글씨까지만 쓴다. */
@Composable
fun RemoveLinkedPlaylistDialog(
    showDialog: Boolean,
    onDismiss: () -> Unit,
    playlistName: String,
    removeFromPlaylist: () -> Unit,
) {
    JellyTubeDialog(
        showDialog = showDialog,
        onDismiss = onDismiss,
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            horizontalAlignment = Alignment.Start,
        ) {
            Text(
                modifier = Modifier.padding(bottom = 16.dp),
                text = "플레이리스트에서 빼기",
                style = MaterialTheme.typography.titleLarge,
            )
            Text(
                modifier = Modifier.padding(bottom = 12.dp),
                // 이름이 사용자 입력이라 조사를 붙이면 받침에서 비문이 된다.
                text = "이 음원을 '$playlistName' 플레이리스트에서 뺍니다." +
                    "\n음원 파일은 지워지지 않아 다시 추가할 수 있습니다.",
                style = MaterialTheme.typography.bodyMedium,
            )
            Row(
                modifier = Modifier.align(Alignment.End)
            ) {
                OutlinedButton(
                    modifier = Modifier.padding(end = 24.dp),
                    contentPadding = PaddingValues(horizontal = 24.dp, vertical = 0.dp),
                    colors = ButtonDefaults.outlinedButtonColors(
                        contentColor = MaterialTheme.colorScheme.onSurface,
                    ),
                    onClick = { onDismiss() },
                ) {
                    Text(
                        text = "취소",
                        fontSize = 14.sp,
                        lineHeight = 14.sp,
                    )
                }
                OutlinedButton(
                    contentPadding = PaddingValues(horizontal = 32.dp, vertical = 0.dp),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = softRed),
                    onClick = {
                        removeFromPlaylist()
                        onDismiss()
                    },
                ) {
                    Text(
                        text = "빼기",
                        fontSize = 14.sp,
                        lineHeight = 14.sp,
                    )
                }
            }
        }
    }
}

@Preview
@Composable
fun RemoveLinkedPlaylistDialogPreview() {
    JellyTubeTheme {
        RemoveLinkedPlaylistDialog(
            showDialog = true,
            onDismiss = {},
            playlistName = "운동할 때 듣는 음악",
            removeFromPlaylist = {},
        )
    }
}
