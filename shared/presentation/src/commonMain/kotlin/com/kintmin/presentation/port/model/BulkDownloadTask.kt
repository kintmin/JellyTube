package com.kintmin.presentation.port.model

/** 화면을 나가도 받던 곡과 정리는 마저 돌므로, 그동안 다른 작업이 겹치지 않게 [runningTask] 로 센다. */
enum class BulkDownloadTask(val runningTask: RunningTask) {
    BACKUP_RESTORE(RunningTask.BACKUP_RESTORE),
    PLAYLIST_DOWNLOAD(RunningTask.PLAYLIST_DOWNLOAD),
}
