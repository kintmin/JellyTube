package com.kintmin.desktop

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Window
import androidx.compose.ui.window.rememberWindowState
import com.kintmin.desktop.resources.Res
import com.kintmin.desktop.resources.icon
import org.jetbrains.compose.resources.painterResource
import java.awt.Dimension

@Composable
fun App(onCloseRequest: () -> Unit) {
    Window(
        onCloseRequest = onCloseRequest,
        title = "JellyTube 파일 공유",
        icon = painterResource(Res.drawable.icon),
        state = rememberWindowState(size = DpSize(1040.dp, 720.dp)),
    ) {
        LaunchedEffect(Unit) {
            // AWT 는 픽셀이 아니라 화면 배율이 적용된 좌표를 쓴다.
            window.minimumSize = Dimension(MIN_WINDOW_WIDTH, MIN_WINDOW_HEIGHT)
        }
        MaterialTheme(colorScheme = if (isSystemInDarkTheme()) darkColorScheme() else lightColorScheme()) {
            MainScreen()
        }
    }
}

private const val MIN_WINDOW_WIDTH = 480
private const val MIN_WINDOW_HEIGHT = 560
