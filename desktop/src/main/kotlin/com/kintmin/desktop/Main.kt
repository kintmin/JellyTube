package com.kintmin.desktop

import androidx.compose.ui.window.application

fun main() = application {
    App(onCloseRequest = ::exitApplication)
}
