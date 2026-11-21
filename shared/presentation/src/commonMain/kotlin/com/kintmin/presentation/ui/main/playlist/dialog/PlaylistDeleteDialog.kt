package com.kintmin.presentation.ui.main.playlist.dialog

import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.Preview
import com.kintmin.presentation.theme.JellyTubeTheme
import com.kintmin.presentation.ui.common.ConfirmStyle
import com.kintmin.presentation.ui.common.JellyTubeConfirmDialog

@Composable
fun PlaylistDeleteDialog(
    showDialog: Boolean,
    onDismiss: () -> Unit,
    playlistName: String,
    audioMediaCount: Int,
    deletePlaylist: () -> Unit,
) {
    JellyTubeConfirmDialog(
        showDialog = showDialog,
        title = "플레이리스트 삭제",
        // 이름이 사용자 입력이라 조사를 붙이면 받침에서 비문이 된다.
        message = buildString {
            append("'$playlistName' 플레이리스트를 지웁니다.")
            if (audioMediaCount > 0) {
                // 곡이 다른 USER 플레이리스트에도 들어 있으면 미분류로 가지 않고 거기 남는다.
                append("\n담긴 ${audioMediaCount}곡은 지워지지 않습니다.")
                append("\n다른 플레이리스트에 없는 곡은 미분류로 갑니다.")
            }
        },
        confirmText = "삭제",
        confirmStyle = ConfirmStyle.Destructive,
        onConfirm = deletePlaylist,
        onDismiss = onDismiss,
    )
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
