package com.kintmin.presentation.platform

import androidx.compose.runtime.Composable

/** 로그 전송 시도 결과. 사용자에게 보여줄 문구는 호출자가 정한다. */
enum class AppLogShareResult {

    Started,
    NoLogFiles,
    NoShareTarget,
    Failed,
}

/**
 * 앱 로그 파일을 외부로 공유한다.
 *
 * 로그 파일을 어디서 찾고 어떤 수단으로 넘기는지는 플랫폼마다 다르다
 * (Android 는 `filesDir` + FileProvider + `ACTION_SEND_MULTIPLE`).
 */
fun interface AppLogSharer {

    fun share(): AppLogShareResult
}

@Composable
expect fun rememberAppLogSharer(): AppLogSharer
