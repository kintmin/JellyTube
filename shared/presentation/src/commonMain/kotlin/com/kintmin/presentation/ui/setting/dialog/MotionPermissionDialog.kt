package com.kintmin.presentation.ui.setting.dialog

import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.Preview
import com.kintmin.presentation.theme.JellyTubeTheme
import com.kintmin.presentation.ui.common.JellyTubeConfirmDialog

/**
 * 걸음수 권한이 거부·제한됐을 때. 한 번 거부되면 OS 가 권한 창을 다시 띄워 주지 않아
 * 설정으로 보내는 것이 유일한 길이다.
 */
@Composable
fun MotionPermissionDialog(
    showDialog: Boolean,
    onOpenSettings: () -> Unit,
    onDismiss: () -> Unit,
) {
    JellyTubeConfirmDialog(
        showDialog = showDialog,
        title = "걸음수를 세려면 권한이 필요합니다",
        message = "설정에서 동작 및 피트니스(신체 활동)를 허용해주세요.\n\n" +
            "앱 설정에 그 항목이 없다면 기기 전체에서 꺼져 있는 것입니다.\n" +
            "설정 > 개인정보 보호 및 보안 > 동작 및 피트니스 > 피트니스 추적을 먼저 켜주세요.",
        dismissText = "나중에",
        confirmText = "설정 열기",
        onConfirm = onOpenSettings,
        onDismiss = onDismiss,
    )
}

@Preview
@Composable
private fun MotionPermissionDialogPreview() {
    JellyTubeTheme {
        MotionPermissionDialog(showDialog = true, onOpenSettings = {}, onDismiss = {})
    }
}
