package com.kintmin.presentation.port

import com.kintmin.presentation.port.model.BulkDownloadTask

/**
 * 화면을 열어 둔 채 여러 곡을 받는 동안 앱이 내려가도 [keepAlive] 의 `block` 이 이어지게 OS 가 허락하는 장치를 건다.
 * `block` 이 도는 동안은 [RunningTaskMonitor] 에 그 작업으로 잡힌다.
 */
interface BulkDownloadKeepAlive {

    suspend fun <T> keepAlive(task: BulkDownloadTask, block: suspend () -> T): T
}
