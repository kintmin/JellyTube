package com.kintmin.presentation.platform

import platform.UIKit.UIApplication

/** `idleTimerDisabled` 는 앱에 하나뿐이라, 붙잡은 쪽이 여럿이면 먼저 놓은 쪽이 남의 것까지 풀어 버린다. */
internal class ScreenAwakeHolds(private val apply: (Boolean) -> Unit) {
    private var count = 0

    fun acquire() {
        if (count++ == 0) apply(true)
    }

    fun release() {
        if (count == 0) return
        if (--count == 0) apply(false)
    }
}

/** 메인 스레드에서만 부른다. */
internal val ScreenAwake = ScreenAwakeHolds { UIApplication.sharedApplication.idleTimerDisabled = it }
