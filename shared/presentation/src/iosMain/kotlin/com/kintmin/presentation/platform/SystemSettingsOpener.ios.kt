package com.kintmin.presentation.platform

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember

/**
 * iOS 는 앱이 시스템 설정의 특정 화면으로 보낼 수 없다.
 *
 * `UIApplication.openSettingsURLString` 은 이 앱의 설정 페이지만 열고,
 * 알람·소리 설정 같은 시스템 앱 목적지로 가는 공개 URL 은 없다.
 * 호출자가 false 를 안내 문구로 처리한다.
 */
@Composable
actual fun rememberSystemSettingsOpener(): SystemSettingsOpener =
    remember { SystemSettingsOpener { false } }
