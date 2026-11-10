package com.kintmin.presentation.platform

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import platform.Foundation.NSURL
import platform.UIKit.UIApplication
import platform.UIKit.UIApplicationOpenSettingsURLString

/**
 * `openSettingsURLString` 으로 열 수 있는 것은 **이 앱의 설정 페이지뿐**이다.
 * 알람·소리 같은 시스템 앱 목적지로 가는 공개 URL 은 없다.
 */
@Composable
actual fun rememberSystemSettingsOpener(): SystemSettingsOpener = remember {
    SystemSettingsOpener { target ->
        when (target) {
            SystemSettingsTarget.AppPermissions -> openAppSettings()
            SystemSettingsTarget.Alarm, SystemSettingsTarget.Sound -> false
        }
    }
}

private fun openAppSettings(): Boolean {
    val url = NSURL.URLWithString(UIApplicationOpenSettingsURLString) ?: return false
    val application = UIApplication.sharedApplication
    if (!application.canOpenURL(url)) return false

    application.openURL(url, emptyMap<Any?, Any?>(), null)
    return true
}
