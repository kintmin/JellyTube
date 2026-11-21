package com.kintmin.presentation.ui.setting.device_save_select.dialog

import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.Preview
import com.kintmin.presentation.theme.JellyTubeTheme
import com.kintmin.presentation.ui.common.JellyTubeConfirmDialog

@Composable
fun RingtoneDefaultChangeDialog(
    isShow: Boolean,
    onDismiss: () -> Unit,
    onConfirmShare: () -> Unit,
) {
    JellyTubeConfirmDialog(
        showDialog = isShow,
        title = "벨소리로 등록",
        message = "'공유 화면 열기'를 누르면 공유 화면이 열립니다. 아래쪽 '더 보기'를 눌러 " +
            "'벨소리로 사용'을 선택해주세요.\n\n" +
            "등록하면 기기의 기본 벨소리가 이 음원으로 바뀝니다. " +
            "알람에만 쓰려면 등록 직후 나오는 화면에서 원래 벨소리를 다시 선택해주세요. " +
            "등록한 음원은 시계 앱의 알람 사운드 목록 '벨소리'에 남아 있습니다.",
        confirmText = "공유 화면 열기",
        onConfirm = onConfirmShare,
        onDismiss = onDismiss,
    )
}

@Preview
@Composable
private fun RingtoneDefaultChangeDialogPreview() {
    JellyTubeTheme {
        RingtoneDefaultChangeDialog(
            isShow = true,
            onDismiss = {},
            onConfirmShare = {},
        )
    }
}
