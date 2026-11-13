package com.kintmin.presentation.platform

import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect

@Composable
actual fun KeepScreenOnEffect(enabled: Boolean) {
    DisposableEffect(enabled) {
        if (enabled) ScreenAwake.acquire()
        onDispose { if (enabled) ScreenAwake.release() }
    }
}
