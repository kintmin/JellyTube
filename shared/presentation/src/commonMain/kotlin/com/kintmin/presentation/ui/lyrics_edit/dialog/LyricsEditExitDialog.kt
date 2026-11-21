package com.kintmin.presentation.ui.lyrics_edit.dialog

import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.Preview
import com.kintmin.presentation.theme.JellyTubeTheme
import com.kintmin.presentation.ui.common.ConfirmStyle
import com.kintmin.presentation.ui.common.JellyTubeConfirmDialog

@Composable
fun LyricsEditExitDialog(
    isShow: Boolean,
    onDismiss: () -> Unit,
    onConfirmExit: () -> Unit,
) {
    JellyTubeConfirmDialog(
        showDialog = isShow,
        title = "편집 나가기",
        message = "저장하지 않은 편집 내용이 사라집니다.",
        dismissText = "계속 편집",
        confirmText = "나가기",
        confirmStyle = ConfirmStyle.Destructive,
        onConfirm = onConfirmExit,
        onDismiss = onDismiss,
    )
}

@Preview
@Composable
private fun LyricsEditExitDialogPreview() {
    JellyTubeTheme {
        LyricsEditExitDialog(
            isShow = true,
            onDismiss = {},
            onConfirmExit = {},
        )
    }
}
