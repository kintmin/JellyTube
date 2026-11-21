package com.kintmin.presentation.ui.audio_media_information_edit.dialog

import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.Preview
import com.kintmin.presentation.theme.JellyTubeTheme
import com.kintmin.presentation.ui.common.ConfirmStyle
import com.kintmin.presentation.ui.common.JellyTubeConfirmDialog

@Composable
fun RemoveLinkedPlaylistDialog(
    showDialog: Boolean,
    onDismiss: () -> Unit,
    playlistName: String,
    removeFromPlaylist: () -> Unit,
) {
    JellyTubeConfirmDialog(
        showDialog = showDialog,
        title = "플레이리스트에서 빼기",
        // 이름이 사용자 입력이라 조사를 붙이면 받침에서 비문이 된다.
        message = "이 음원을 '$playlistName' 플레이리스트에서 뺍니다." +
            "\n음원 파일은 지워지지 않아 다시 추가할 수 있습니다.",
        confirmText = "빼기",
        confirmStyle = ConfirmStyle.Removal,
        onConfirm = removeFromPlaylist,
        onDismiss = onDismiss,
    )
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
