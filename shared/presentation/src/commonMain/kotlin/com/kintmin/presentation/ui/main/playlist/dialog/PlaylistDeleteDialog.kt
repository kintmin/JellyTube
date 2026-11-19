package com.kintmin.presentation.ui.main.playlist.dialog

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
import androidx.compose.ui.unit.sp
import com.kintmin.presentation.theme.JellyTubeTheme
import com.kintmin.presentation.ui.common.JellyTubeDialog

@Composable
fun PlaylistDeleteDialog(
    showDialog: Boolean,
    onDismiss: () -> Unit,
    playlistName: String,
    audioMediaCount: Int,
    deletePlaylist: () -> Unit,
) {
    JellyTubeDialog(
        showDialog = showDialog,
        onDismiss = onDismiss,
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            horizontalAlignment = Alignment.Start
        ) {
            Text(
                modifier = Modifier.padding(bottom = 16.dp),
                text = "플레이리스트 삭제",
                style = MaterialTheme.typography.titleLarge,
            )
            Text(
                modifier = Modifier.padding(bottom = 12.dp),
                // 이름이 사용자 입력이라 조사를 붙이면 받침에서 비문이 된다.
                text = "'$playlistName' 플레이리스트를 지웁니다.\n" +
                    "담긴 ${audioMediaCount}곡은 지워지지 않고 미분류로 돌아갑니다.",
                style = MaterialTheme.typography.bodyMedium,
            )
            Row(
                modifier = Modifier.align(Alignment.End)
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
                        text = "취소",
                        fontSize = 14.sp,
                        lineHeight = 14.sp,
                    )
                }
                Button(
                    contentPadding = PaddingValues(horizontal = 32.dp, vertical = 0.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.error,
                        contentColor = MaterialTheme.colorScheme.onError,
                    ),
                    onClick = {
                        deletePlaylist()
                        onDismiss()
                    },
                ) {
                    Text(
                        text = "삭제하기",
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
fun PlaylistDeleteDialogPreview() {
    JellyTubeTheme {
        PlaylistDeleteDialog(
            showDialog = true,
            onDismiss = {},
            playlistName = "플레이리스트 예시",
            audioMediaCount = 12,
            deletePlaylist = {},
        )
    }
}
