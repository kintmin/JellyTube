package com.kintmin.presentation.port

import platform.UIKit.UIApplication
import platform.UIKit.UIBackgroundTaskInvalid

/**
 * 앱이 백그라운드로 내려가도 하던 일을 마치게 OS 에 유예를 요청한다.
 *
 * iOS 는 백그라운드로 간 앱을 곧 서스펜드하고, 그러면 코루틴도 네트워크 요청도 그 자리에서
 * 멈춘다. 음원 바이트는 `URLSession` 이 받아 주지만 썸네일 받기·DB 기록은 앱이 해야 한다.
 */
internal suspend fun <T> withBackgroundExecutionWindow(name: String, block: suspend () -> T): T {
    val application = UIApplication.sharedApplication
    var identifier = UIBackgroundTaskInvalid

    // 유예를 반납하지 않고 시간을 넘기면 OS 가 앱을 죽인다. 만료 직전에 돌려줄 자리를 둬야 한다.
    identifier = application.beginBackgroundTaskWithName(name) {
        val current = identifier
        if (current != UIBackgroundTaskInvalid) {
            identifier = UIBackgroundTaskInvalid
            application.endBackgroundTask(current)
        }
    }

    try {
        return block()
    } finally {
        val current = identifier
        if (current != UIBackgroundTaskInvalid) {
            identifier = UIBackgroundTaskInvalid
            application.endBackgroundTask(current)
        }
    }
}
