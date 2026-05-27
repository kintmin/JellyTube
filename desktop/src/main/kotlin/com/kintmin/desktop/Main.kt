package com.kintmin.desktop

import androidx.compose.ui.window.application

// ./gradlew packageDistributionForCurrentOS
fun main() = application {
    App(onCloseRequest = ::exitApplication)
}
