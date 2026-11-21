package com.kintmin.presentation.ui.setting.device_save_select.dialog

import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.Preview
import com.kintmin.presentation.theme.JellyTubeTheme
import com.kintmin.presentation.ui.common.JellyTubeConfirmDialog

@Composable
fun RingtoneTooLongDialog(
    isShow: Boolean,
    onDismiss: () -> Unit,
    onConfirmEdit: () -> Unit,
) {
    JellyTubeConfirmDialog(
        showDialog = isShow,
        title = "30초 미만이어야 합니다",
        message = "벨소리·알람음으로는 30초 미만의 음원만 등록할 수 있습니다. 음원을 편집해서 줄이시겠습니까?",
        confirmText = "편집",
        onConfirm = onConfirmEdit,
        onDismiss = onDismiss,
    )
}

@Preview
@Composable
private fun RingtoneTooLongDialogPreview() {
    JellyTubeTheme {
        RingtoneTooLongDialog(
            isShow = true,
            onDismiss = {},
            onConfirmEdit = {},
        )
    }
}
