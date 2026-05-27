package com.kintmin.desktop

import androidx.compose.runtime.Composable
import androidx.compose.ui.window.Window
import com.kintmin.desktop.resources.Res
import com.kintmin.desktop.resources.icon
import org.jetbrains.compose.resources.painterResource

@Composable
fun App(onCloseRequest: () -> Unit) {
    Window(
        onCloseRequest = onCloseRequest,
        title = "JellyTube 파일 공유",
        icon = painterResource(Res.drawable.icon),
    ) {
        MainScreen()
    }
}
