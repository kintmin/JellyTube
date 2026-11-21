package com.kintmin.jellytube.service_controller

import android.content.Context
import com.kintmin.jellytube.service.FileShareForegroundService
import com.kintmin.presentation.port.RestoreKeepAlive

/** 서비스를 못 켜도 복원은 막지 않는다. 앱이 내려가도 받던 곡을 마저 받는다. */
class RestoreKeepAliveImpl(
    private val appContext: Context,
) : RestoreKeepAlive {

    override suspend fun <T> keepAlive(block: suspend () -> T): T {
        FileShareForegroundService.startService(appContext, FileShareForegroundService.Owner.BACKUP_RESTORE)
        try {
            return block()
        } finally {
            FileShareForegroundService.stopService(appContext, FileShareForegroundService.Owner.BACKUP_RESTORE)
        }
    }
}
