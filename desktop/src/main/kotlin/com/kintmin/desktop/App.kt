package com.kintmin.desktop

import androidx.compose.runtime.Composable
import androidx.compose.ui.window.Window

@Composable
fun App(onCloseRequest: () -> Unit) {
    Window(
        onCloseRequest = onCloseRequest,
        title = "JellyTube 파일 공유",
    ) {
        MainScreen()
    }
}
