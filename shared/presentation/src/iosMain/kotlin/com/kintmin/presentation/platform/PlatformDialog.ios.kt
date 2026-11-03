package com.kintmin.presentation.platform

import androidx.compose.runtime.Composable
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties

@Composable
actual fun PlatformFullScreenDialog(
    onDismissRequest: () -> Unit,
    hideSystemBars: Boolean,
    content: @Composable () -> Unit,
) {
    // iOS 는 창 개념이 없어 usePlatformDefaultWidth 만 끄면 화면 전체를 받는다.
    // Android 의 창 배경·DIM·decor 처리에 대응하는 것이 없다.
    // hideSystemBars 도 대응물이 없다. 상태바 숨김이 필요해지면
    // UIViewController.prefersStatusBarHidden 을 건드려야 하는데,
    // ComposeUIViewController 바깥의 Swift 쪽 작업이라 iOS 화면 작업과 함께 다룬다.
    Dialog(
        onDismissRequest = onDismissRequest,
        properties = DialogProperties(usePlatformDefaultWidth = false),
    ) {
        content()
    }
}

@Composable
actual fun PlatformBottomSheetSystemBarEffect(lightNavigationBarIcons: Boolean) {
    // iOS 에는 내비게이션바가 없다.
}
