package com.kintmin.data.file_share

import platform.UIKit.UIApplication
import platform.UIKit.UIBackgroundTaskInvalid

/**
 * 유예는 길어야 수십 초라, 받던 파일을 마치는 정도다. presentation 의 같은 도우미는 data 가 볼 수 없어
 * 여기 따로 둔다.
 */
internal actual suspend fun <T> continueInBackgroundWhile(block: suspend () -> T): T {
    val application = UIApplication.sharedApplication
    var identifier = UIBackgroundTaskInvalid

    // 유예를 반납하지 않고 시간을 넘기면 OS 가 앱을 죽인다. 만료 직전에 돌려줄 자리를 둬야 한다.
    identifier = application.beginBackgroundTaskWithName(WINDOW_NAME) {
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

private const val WINDOW_NAME = "pc-upload"
