package com.kintmin.presentation.ui.playlist_edit.dialog

import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.Preview
import com.kintmin.presentation.theme.JellyTubeTheme
import com.kintmin.presentation.ui.common.ConfirmStyle
import com.kintmin.presentation.ui.common.JellyTubeConfirmDialog

/**
 * 음원 파일 삭제 확인. [audioMediaName] 을 주면 단건으로, 주지 않으면 [selectedMediaCount] 건으로 묻는다.
 * [canRedownload] 가 false 면 다시 받을 곳이 없다고 알린다 — 기기에서 가져온 음원이 그렇다.
 */
@Composable
fun DeleteFullAudioMediaListDialog(
    isShow: Boolean,
    onDismiss: () -> Unit,
    selectedMediaCount: Int,
    deleteAudioMediaList: () -> Unit,
    audioMediaName: String? = null,
    canRedownload: Boolean = true,
) {
    JellyTubeConfirmDialog(
        showDialog = isShow,
        title = "음원 파일 삭제",
        message = buildString {
            if (audioMediaName != null) {
                // 이름이 사용자 입력이라 조사를 붙이면 받침에서 비문이 된다.
                append("'$audioMediaName' 음원 파일을 지웁니다.")
            } else {
                append("음원 파일 ${selectedMediaCount}개를 지웁니다.")
            }
            append("\n모든 플레이리스트에서 사라집니다.")
            if (canRedownload) {
                append("\n다시 다운로드해야 재생할 수 있습니다.")
            } else {
                append("\n기기에서 가져온 음원이라 다시 받을 수 없습니다.")
            }
        },
        confirmText = "삭제",
        confirmStyle = ConfirmStyle.Destructive,
        onConfirm = deleteAudioMediaList,
        onDismiss = onDismiss,
    )
}

@Preview
@Composable
fun DeleteFullAudioMediaListDialogPreview() {
    JellyTubeTheme {
        DeleteFullAudioMediaListDialog(
            isShow = true,
            onDismiss = {},
            selectedMediaCount = 4,
            deleteAudioMediaList = {},
        )
    }
}

@Preview
@Composable
fun DeleteSingleAudioMediaDialogPreview() {
    JellyTubeTheme {
        DeleteFullAudioMediaListDialog(
            isShow = true,
            onDismiss = {},
            selectedMediaCount = 1,
            deleteAudioMediaList = {},
            audioMediaName = "밤편지",
        )
    }
}

@Preview
@Composable
fun DeleteNotRedownloadableAudioMediaDialogPreview() {
    JellyTubeTheme {
        DeleteFullAudioMediaListDialog(
            isShow = true,
            onDismiss = {},
            selectedMediaCount = 1,
            deleteAudioMediaList = {},
            audioMediaName = "PC 에서 받은 녹음",
            canRedownload = false,
        )
    }
}
