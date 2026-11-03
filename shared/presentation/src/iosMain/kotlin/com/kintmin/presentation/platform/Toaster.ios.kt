package com.kintmin.presentation.platform

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember

/**
 * iOS 에는 Toast 대응물이 없다.
 *
 * 공통 Snackbar 로 대체할 때까지 메시지를 버린다. 메시지를 못 보는 것이
 * iOS 다운 UI 를 흉내낸 가짜 Toast 보다 낫다고 보고, 흔적은 남긴다.
 */
private class IosToaster : Toaster {

    override fun show(message: String) {
        println("[Toaster] $message")
    }

    override fun showLong(message: String) {
        println("[Toaster] $message")
    }
}

@Composable
actual fun rememberToaster(): Toaster = remember { IosToaster() }
