package com.kintmin.presentation.ui.playlist_detail.dialog

import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.Preview
import com.kintmin.presentation.theme.JellyTubeTheme
import com.kintmin.presentation.ui.common.ConfirmStyle
import com.kintmin.presentation.ui.common.JellyTubeConfirmDialog

/**
 * [mediaName] 을 주면 단건으로, 주지 않으면 [selectedMediaCount] 건으로 묻는다.
 * [isSecretPlaylist] 면 곡이 어디로 가는지까지 알린다 — 비밀에서 빠지면 드러나는 곳으로 간다.
 */
@Composable
fun RemoveAudioMediaFromPlaylistDialog(
    showDialog: Boolean,
    onDismiss: () -> Unit,
    removeFromPlaylist: () -> Unit,
    mediaName: String? = null,
    selectedMediaCount: Int = 1,
    isSecretPlaylist: Boolean = false,
) {
    JellyTubeConfirmDialog(
        showDialog = showDialog,
        title = "플레이리스트에서 빼기",
        message = buildString {
            if (mediaName != null) {
                // 이름이 사용자 입력이라 조사를 붙이면 받침에서 비문이 된다.
                append("'$mediaName' 음원을 이 플레이리스트에서 뺍니다.")
            } else {
                append("${selectedMediaCount}곡을 이 플레이리스트에서 뺍니다.")
            }
            if (isSecretPlaylist) {
                append("\n다른 비밀 플레이리스트에 없으면 미분류로 가고 '전체' 에도 나타납니다.")
            }
            append("\n음원 파일은 지워지지 않아 다시 추가할 수 있습니다.")
        },
        confirmText = "빼기",
        confirmStyle = ConfirmStyle.Removal,
        onConfirm = removeFromPlaylist,
        onDismiss = onDismiss,
    )
}

@Preview
@Composable
fun RemoveAudioMediaFromPlaylistDialogPreview() {
    JellyTubeTheme {
        RemoveAudioMediaFromPlaylistDialog(
            showDialog = true,
            onDismiss = {},
            removeFromPlaylist = {},
            mediaName = "음원 제목 예시",
        )
    }
}

@Preview
@Composable
fun RemoveAudioMediaFromSecretPlaylistDialogPreview() {
    JellyTubeTheme {
        RemoveAudioMediaFromPlaylistDialog(
            showDialog = true,
            onDismiss = {},
            removeFromPlaylist = {},
            selectedMediaCount = 3,
            isSecretPlaylist = true,
        )
    }
}
