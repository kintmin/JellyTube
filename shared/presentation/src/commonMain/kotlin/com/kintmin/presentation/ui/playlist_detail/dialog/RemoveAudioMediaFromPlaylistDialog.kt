package com.kintmin.presentation.ui.playlist_detail.dialog

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.Preview
import com.kintmin.presentation.theme.JellyTubeTheme
import com.kintmin.presentation.ui.common.ConfirmStyle
import com.kintmin.presentation.ui.common.DialogChoiceButtonRow
import com.kintmin.presentation.ui.common.DialogColumn
import com.kintmin.presentation.ui.common.JellyTubeConfirmDialog
import com.kintmin.presentation.ui.common.JellyTubeDialog

/**
 * [mediaName] 을 주면 단건으로, 주지 않으면 [selectedMediaCount] 건으로 묻는다.
 *
 * **파일까지 지우는 선택지는 비밀 플레이리스트에만 둔다.** 일반 플레이리스트에서 곡을 빼는
 * 사람은 빼려는 것이지 지우려는 것이 아니라서, 파일 삭제는 더보기와 전용 버튼 뒤로 일부러
 * 멀리 두었다. 여기에 삭제 버튼을 나란히 놓으면 되돌릴 수 없는 쪽이 손에 더 가까워진다.
 */
@Composable
fun RemoveAudioMediaFromPlaylistDialog(
    showDialog: Boolean,
    onDismiss: () -> Unit,
    removeFromPlaylist: () -> Unit,
    deleteAudioMedia: () -> Unit,
    mediaName: String? = null,
    selectedMediaCount: Int = 1,
    isSecretPlaylist: Boolean = false,
) {
    // 이름이 사용자 입력이라 조사를 붙이면 받침에서 비문이 된다.
    val target = mediaName?.let { "'$it' 음원을" } ?: "${selectedMediaCount}곡을"

    if (!isSecretPlaylist) {
        JellyTubeConfirmDialog(
            showDialog = showDialog,
            title = "플레이리스트에서 빼기",
            message = "$target 이 플레이리스트에서 뺍니다." +
                "\n음원 파일은 지워지지 않아 다시 추가할 수 있습니다.",
            confirmText = "빼기",
            confirmStyle = ConfirmStyle.Removal,
            onConfirm = removeFromPlaylist,
            onDismiss = onDismiss,
        )
        return
    }

    JellyTubeDialog(showDialog = showDialog, onDismiss = onDismiss) {
        DialogColumn(title = "플레이리스트에서 빼기") {
            Text(
                text = "$target 이 플레이리스트에서 뺍니다." +
                    "\n\n곡을 남기면 음원 파일은 그대로지만, 다른 비밀 플레이리스트에 없으면" +
                    " 미분류로 가고 '전체' 에도 나타납니다." +
                    "\n곡까지 지우면 파일이 함께 삭제되며 모든 플레이리스트에서 사라집니다.",
                style = MaterialTheme.typography.bodyMedium,
            )
            DialogChoiceButtonRow(
                onDismiss = onDismiss,
                removalText = "곡은 남기기",
                onRemoval = removeFromPlaylist,
                destructiveText = if (mediaName != null) "삭제" else "모두 삭제",
                onDestructive = deleteAudioMedia,
            )
        }
    }
}

@Preview
@Composable
fun RemoveAudioMediaFromPlaylistDialogPreview() {
    JellyTubeTheme {
        RemoveAudioMediaFromPlaylistDialog(
            showDialog = true,
            onDismiss = {},
            removeFromPlaylist = {},
            deleteAudioMedia = {},
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
            deleteAudioMedia = {},
            selectedMediaCount = 3,
            isSecretPlaylist = true,
        )
    }
}
