package com.kintmin.presentation.platform

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import platform.Foundation.NSURL

/**
 * 로그 파일을 공유 시트로 넘긴다. 수신자를 지정할 공개 수단이 없어 어디로 보낼지는
 * 사용자가 고르고, 시트가 항상 떠서 `NoShareTarget` 이 서지 않는다.
 */
private fun shareAppLogs(fileFullPaths: List<String>): AppLogShareResult {
    if (fileFullPaths.isEmpty()) return AppLogShareResult.NoLogFiles
    val isStarted = presentShareSheet(fileFullPaths.map { NSURL.fileURLWithPath(it) })
    return if (isStarted) AppLogShareResult.Started else AppLogShareResult.Failed
}

@Composable
actual fun rememberAppLogSharer(): AppLogSharer = remember { AppLogSharer { shareAppLogs(it) } }
