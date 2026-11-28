package com.kintmin.presentation.platform

import platform.UIKit.UIApplication

/** `idleTimerDisabled` 는 앱에 하나뿐이라 붙잡은 수를 센다. 메인 스레드에서만 부른다. */
internal object ScreenAwake {
    private val holds = ScreenAwakeHolds()

    fun acquire() {
        if (holds.acquire()) UIApplication.sharedApplication.idleTimerDisabled = true
    }

    fun release() {
        if (holds.release()) UIApplication.sharedApplication.idleTimerDisabled = false
    }
}
