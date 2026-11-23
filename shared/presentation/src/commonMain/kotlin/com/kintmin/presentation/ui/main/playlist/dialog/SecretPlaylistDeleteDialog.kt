package com.kintmin.presentation.ui.main.playlist.dialog

import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import com.kintmin.presentation.theme.JellyTubeButtonColors
import com.kintmin.presentation.theme.JellyTubeTheme
import com.kintmin.presentation.theme.dialogActionsSpacing
import com.kintmin.presentation.theme.dialogButtonSpacing
import com.kintmin.presentation.ui.common.DialogColumn
import com.kintmin.presentation.ui.common.JellyTubeDialog

/**
 * 곡을 남기면 미분류로 돌아가 "전체" 에도 나타난다 — 비밀로 담아 둔 것이 한 번에 드러나므로
 * 묻지 않고 지우지 않는다.
 */
@Composable
fun SecretPlaylistDeleteDialog(
    showDialog: Boolean,
    onDismiss: () -> Unit,
    playlistName: String,
    audioMediaCount: Int,
    deletePlaylist: () -> Unit,
    deletePlaylistWithTracks: () -> Unit,
) {
    JellyTubeDialog(
        showDialog = showDialog,
        onDismiss = onDismiss,
    ) {
        DialogColumn(title = "비밀 플레이리스트 삭제") {
            Text(
                // 이름이 사용자 입력이라 조사를 붙이면 받침에서 비문이 된다.
                text = buildString {
                    append("'$playlistName' 플레이리스트를 지웁니다.")
                    if (audioMediaCount > 0) {
                        append("\n\n담긴 ${audioMediaCount}곡을 남기면 미분류로 가고 '전체' 에도 나타납니다.")
                        append("\n곡까지 지우면 파일이 함께 삭제되며 되돌릴 수 없습니다.")
                    }
                },
                style = MaterialTheme.typography.bodyMedium,
            )
            Row(
                modifier = Modifier
                    .align(Alignment.End)
                    .padding(top = dialogActionsSpacing),
            ) {
                TextButton(onClick = onDismiss) { Text("취소") }
                Spacer(modifier = Modifier.width(dialogButtonSpacing))
                OutlinedButton(
                    colors = JellyTubeButtonColors.removal(),
                    onClick = {
                        deletePlaylist()
                        onDismiss()
                    },
                ) {
                    Text(text = if (audioMediaCount > 0) "곡은 남기기" else "삭제")
                }
                if (audioMediaCount > 0) {
                    Spacer(modifier = Modifier.width(dialogButtonSpacing))
                    Button(
                        colors = JellyTubeButtonColors.destructive(),
                        onClick = {
                            deletePlaylistWithTracks()
                            onDismiss()
                        },
                    ) {
                        Text(text = "곡까지 삭제")
                    }
                }
            }
        }
    }
}

@Preview
@Composable
fun SecretPlaylistDeleteDialogPreview() {
    JellyTubeTheme {
        SecretPlaylistDeleteDialog(
            showDialog = true,
            onDismiss = {},
            playlistName = "나만 듣는 곡",
            audioMediaCount = 5,
            deletePlaylist = {},
            deletePlaylistWithTracks = {},
        )
    }
}
