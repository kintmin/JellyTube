package com.kintmin.presentation.ui.lyrics_viewer.dialog

import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.Preview
import com.kintmin.presentation.theme.JellyTubeTheme
import com.kintmin.presentation.ui.common.ConfirmStyle
import com.kintmin.presentation.ui.common.JellyTubeConfirmDialog

@Composable
fun LyricsDeleteDialog(
    isShow: Boolean,
    audioMediaName: String,
    onDismiss: () -> Unit,
    onConfirmDelete: () -> Unit,
) {
    JellyTubeConfirmDialog(
        showDialog = isShow,
        title = "가사 삭제",
        // 이름이 사용자 입력이라 조사를 붙이면 받침에서 비문이 된다.
        message = "'$audioMediaName' 가사를 지웁니다." +
            "\n다시 보려면 가사를 다시 검색해야 합니다.",
        confirmText = "삭제",
        confirmStyle = ConfirmStyle.Destructive,
        onConfirm = onConfirmDelete,
        onDismiss = onDismiss,
    )
}

@Preview
@Composable
private fun LyricsDeleteDialogPreview() {
    JellyTubeTheme {
        LyricsDeleteDialog(
            isShow = true,
            audioMediaName = "밤편지",
            onDismiss = {},
            onConfirmDelete = {},
        )
    }
}
