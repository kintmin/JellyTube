package com.kintmin.presentation.platform

import androidx.compose.runtime.Composable

/** 사용자에게 보여줄 앱 버전 문자열. 읽을 수 없으면 빈 문자열. */
@Composable
expect fun rememberAppVersionName(): String
