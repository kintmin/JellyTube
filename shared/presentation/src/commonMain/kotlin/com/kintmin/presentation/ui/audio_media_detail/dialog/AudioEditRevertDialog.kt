package com.kintmin.presentation.ui.audio_media_detail.dialog

import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.Preview
import com.kintmin.presentation.theme.JellyTubeTheme
import com.kintmin.presentation.ui.common.ConfirmStyle
import com.kintmin.presentation.ui.common.JellyTubeConfirmDialog

@Composable
fun AudioEditRevertDialog(
    isShow: Boolean,
    onDismiss: () -> Unit,
    onConfirmRevert: () -> Unit,
) {
    JellyTubeConfirmDialog(
        showDialog = isShow,
        title = "원본으로 되돌리기",
        message = "편집본을 지우고 원본 음원으로 되돌립니다. 원본은 그대로 남아있어 다시 편집할 수 있습니다.",
        confirmText = "되돌리기",
        confirmStyle = ConfirmStyle.Destructive,
        onConfirm = onConfirmRevert,
        onDismiss = onDismiss,
    )
}

@Preview
@Composable
private fun AudioEditRevertDialogPreview() {
    JellyTubeTheme {
        AudioEditRevertDialog(
            isShow = true,
            onDismiss = {},
            onConfirmRevert = {},
        )
    }
}
