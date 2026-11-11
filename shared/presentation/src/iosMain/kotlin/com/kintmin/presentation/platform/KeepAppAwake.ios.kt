package com.kintmin.presentation.platform

import com.kintmin.presentation.port.withBackgroundExecutionWindow
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.withContext
import platform.UIKit.UIApplication

/**
 * 화면이 꺼지면 앱이 서스펜드돼 전송이 멈춘다. 유예는 길어야 수십 초라 긴 작업의 주된 수단이
 * 아니고, 잠깐 다른 앱을 보고 오는 동안 하던 파일을 마치는 정도다.
 */
actual suspend fun <T> keepAppAwakeWhile(block: suspend () -> T): T {
    setIdleTimerDisabled(true)
    try {
        return withBackgroundExecutionWindow(WINDOW_NAME) { block() }
    } finally {
        withContext(NonCancellable) { setIdleTimerDisabled(false) }
    }
}

private suspend fun setIdleTimerDisabled(disabled: Boolean) = withContext(Dispatchers.Main) {
    UIApplication.sharedApplication.idleTimerDisabled = disabled
}

private const val WINDOW_NAME = "data-migration"
