package com.kintmin.jellytube.service_controller

import android.content.Context
import com.kintmin.jellytube.service.FileShareForegroundService
import com.kintmin.presentation.port.BulkDownloadKeepAlive
import com.kintmin.presentation.port.model.BulkDownloadTask
import com.kintmin.presentation.port.model.RunningTask
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.update

/** 서비스를 못 켜도 받기는 막지 않는다. 앱이 내려가도 받던 곡을 마저 받는다. */
class BulkDownloadKeepAliveImpl(
    private val appContext: Context,
) : BulkDownloadKeepAlive {

    private val counts = MutableStateFlow<Map<RunningTask, Int>>(emptyMap())

    override suspend fun <T> keepAlive(task: BulkDownloadTask, block: suspend () -> T): T {
        val owner = when (task) {
            BulkDownloadTask.BACKUP_RESTORE -> FileShareForegroundService.Owner.BACKUP_RESTORE
            BulkDownloadTask.PLAYLIST_DOWNLOAD -> FileShareForegroundService.Owner.PLAYLIST_DOWNLOAD
        }
        counts.update { it + (task.runningTask to (it[task.runningTask] ?: 0) + 1) }
        FileShareForegroundService.startService(appContext, owner)
        try {
            return block()
        } finally {
            FileShareForegroundService.stopService(appContext, owner)
            counts.update { it + (task.runningTask to (it[task.runningTask] ?: 1) - 1) }
        }
    }

    fun runningTasks(): Map<RunningTask, Int> = counts.value
}
