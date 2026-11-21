package com.kintmin.presentation.ui.audio_media_detail.dialog

import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.Preview
import com.kintmin.presentation.theme.JellyTubeTheme
import com.kintmin.presentation.ui.common.ConfirmStyle
import com.kintmin.presentation.ui.common.JellyTubeConfirmDialog

@Composable
fun KaraokeUnlinkDialog(
    isShow: Boolean,
    onDismiss: () -> Unit,
    onConfirmUnlink: () -> Unit,
) {
    JellyTubeConfirmDialog(
        showDialog = isShow,
        title = "노래방 번호 연동 해제",
        message = "연동된 TJ 미디어 노래방 번호를 연동 해제하시겠습니까?",
        confirmText = "해제",
        confirmStyle = ConfirmStyle.Removal,
        onConfirm = onConfirmUnlink,
        onDismiss = onDismiss,
    )
}

@Preview
@Composable
private fun KaraokeUnlinkDialogPreview() {
    JellyTubeTheme {
        KaraokeUnlinkDialog(
            isShow = true,
            onDismiss = {},
            onConfirmUnlink = {},
        )
    }
}
