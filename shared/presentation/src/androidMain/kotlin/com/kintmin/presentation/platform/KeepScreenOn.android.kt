package com.kintmin.presentation.platform

import android.view.View
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.ui.platform.LocalView
import java.util.WeakHashMap

// 뷰는 윈도우에 하나뿐이라 두 화면이 함께 붙잡으면 먼저 떠나는 쪽이 남의 것까지 풀어 버린다.
// 값이 뷰를 참조하지 않아 떠난 윈도우의 항목은 제때 비워진다.
private val holdsByView = WeakHashMap<View, ScreenAwakeHolds>()

@Composable
actual fun KeepScreenOnEffect(enabled: Boolean) {
    val view = LocalView.current
    DisposableEffect(view, enabled) {
        if (enabled) view.acquireKeepScreenOn()
        onDispose { if (enabled) view.releaseKeepScreenOn() }
    }
}

// 부르는 자리가 Compose 이펙트뿐이라 메인 스레드다 — 맵에 잠금을 두지 않는다.
private fun View.acquireKeepScreenOn() {
    if (holdsByView.getOrPut(this) { ScreenAwakeHolds() }.acquire()) keepScreenOn = true
}

private fun View.releaseKeepScreenOn() {
    val holds = holdsByView[this] ?: return
    if (!holds.release()) return
    keepScreenOn = false
    holdsByView.remove(this)
}
