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
 * 앱 로그 파일을 외부로 공유한다. 파일 목록은 data 가 주고, 여기서는 넘기는 수단만 플랫폼마다 다르다
 * (Android 는 FileProvider + `ACTION_SEND_MULTIPLE`, iOS 는 공유 시트).
 */
fun interface AppLogSharer {

    fun share(fileFullPaths: List<String>): AppLogShareResult
}

@Composable
expect fun rememberAppLogSharer(): AppLogSharer
