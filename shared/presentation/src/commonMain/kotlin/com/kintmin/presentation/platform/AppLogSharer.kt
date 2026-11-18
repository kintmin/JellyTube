package com.kintmin.presentation.platform

import androidx.compose.runtime.Composable

/** 로그 전송 시도 결과. 사용자에게 보여줄 문구는 호출자가 정한다. */
enum class AppLogShareResult {

    Started,
    NoLogFiles,
    NoShareTarget,
    Failed,
}

fun interface AppLogSharer {

    fun share(fileFullPaths: List<String>): AppLogShareResult
}

@Composable
expect fun rememberAppLogSharer(): AppLogSharer
