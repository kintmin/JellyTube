package com.kintmin.presentation.platform

import androidx.compose.runtime.Composable

/** 짧은 안내 메시지. Android 는 네이티브 `Toast`, iOS 는 [PlatformToastHost] 가 그린다. */
interface Toaster {

    fun show(message: String)

    fun showLong(message: String)
}

@Composable
expect fun rememberToaster(): Toaster

/**
 * 앱 루트에 **한 번만** 깐다. Android 는 no-op.
 *
 * 전체화면 다이얼로그는 별도 레이어라 그 위에는 올라가지 않는다.
 */
@Composable
expect fun PlatformToastHost()
