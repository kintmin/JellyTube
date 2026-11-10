package com.kintmin.presentation.platform

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember

/**
 * iOS 는 앱이 시스템 설정의 특정 화면으로 보낼 수 없다. `openSettingsURLString` 은 이 앱의
 * 설정 페이지만 열고, 알람·소리 설정으로 가는 공개 URL 은 없다.
 */
@Composable
actual fun rememberSystemSettingsOpener(): SystemSettingsOpener =
    remember { SystemSettingsOpener { false } }
