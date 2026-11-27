package com.kintmin.presentation.ui.main.notice

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.Preview
import com.kintmin.presentation.theme.JellyTubeTheme
import com.kintmin.presentation.ui.common.DialogButtonRow
import com.kintmin.presentation.ui.common.DialogColumn
import com.kintmin.presentation.ui.common.JellyTubeDialog

/**
 * 권한 창을 바로 띄우지 않고 무엇이 좋아지는지를 먼저 말한다 — **한 번 거절되면 OS 가 그 창을
 * 다시 띄워 주지 않아** 되돌리려면 사용자가 설정까지 찾아가야 한다.
 *
 * 밖을 눌러도 닫히지 않는다. 묻는 것이 한 번뿐이라 스치듯 사라지면 그걸로 끝이다.
 */
@Composable
fun NotificationNoticeDialog(
    showDialog: Boolean,
    onAllow: () -> Unit,
    onDismiss: () -> Unit,
) {
    JellyTubeDialog(
        showDialog = showDialog,
        onDismiss = onDismiss,
        dismissOnClickOutside = false,
    ) {
        DialogColumn(title = "받기가 끝나면 알려 드릴까요?") {
            Text(
                text = "음원을 받는 동안 다른 일을 하셔도 됩니다. 다 받으면 알림으로 알려 드려요.",
                style = MaterialTheme.typography.bodyMedium,
            )
            DialogButtonRow(
                dismissText = "나중에",
                onDismiss = onDismiss,
                confirmText = "알림 받기",
                onConfirm = onAllow,
            )
        }
    }
}

@Preview
@Composable
private fun NotificationNoticeDialogPreview() {
    JellyTubeTheme {
        NotificationNoticeDialog(showDialog = true, onAllow = {}, onDismiss = {})
    }
}
