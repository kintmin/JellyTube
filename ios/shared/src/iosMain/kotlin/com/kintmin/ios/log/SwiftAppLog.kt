package com.kintmin.ios.log

import com.kintmin.log.AppLog
import com.kintmin.log.model.DebugLog
import com.kintmin.log.model.FirebaseEvent
import org.koin.mp.KoinPlatform

/**
 * Swift 가 공유 앱 로그에 쓰는 자리.
 * 파일명이 Obj-C 클래스명을 정하므로 이 파일 이름을 바꾸면 Swift 호출부가 깨진다.
 *
 * Swift 가 `os_log` 로만 남기면 `Documents/app_logs` 에 가지 않는다. 로그 화면과 로그 공유가
 * 그 파일만 읽고, 기기에서 뽑아 볼 수 있는 것도 그 파일뿐이다.
 */
fun swiftDebugLog(tag: String, message: String) {
    // 로그 한 줄 때문에 앱이 죽어선 안 된다. Koin 이 서기 전에 불릴 수 있다.
    runCatching { KoinPlatform.getKoin().get<AppLog>() }
        .getOrNull()
        ?.sendDebugLog(DebugLog(tag, message))
}

/** HLS fallback 건수를 세는 이벤트와 같은 내용의 디버그 로그를 함께 남긴다. */
fun swiftYoutubeDownloadFallbackLog(source: String, reason: String, formatId: String, transferProtocol: String) {
    val appLog = runCatching { KoinPlatform.getKoin().get<AppLog>() }.getOrNull() ?: return
    appLog.sendFirebaseEvent(FirebaseEvent.YoutubeDownloadFallback(source, reason, formatId, transferProtocol))
    appLog.sendDebugLog(
        DebugLog("YoutubeHlsFallback", "reason=$reason format=$formatId protocol=$transferProtocol source=$source"),
    )
}
