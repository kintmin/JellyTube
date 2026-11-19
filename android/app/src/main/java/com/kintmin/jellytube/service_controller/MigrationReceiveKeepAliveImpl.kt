package com.kintmin.jellytube.service_controller

import android.content.Context
import com.kintmin.jellytube.service.FileShareForegroundService
import com.kintmin.presentation.port.MigrationReceiveKeepAlive

/** 서비스를 못 켜도 받기는 막지 않는다. 앱이 내려가면 받던 것을 이어 받는다. */
class MigrationReceiveKeepAliveImpl(
    private val appContext: Context,
) : MigrationReceiveKeepAlive {

    override suspend fun <T> keepAlive(block: suspend () -> T): T {
        FileShareForegroundService.startService(appContext, FileShareForegroundService.Owner.DATA_MIGRATION_RECEIVE)
        try {
            return block()
        } finally {
            FileShareForegroundService.stopService(appContext, FileShareForegroundService.Owner.DATA_MIGRATION_RECEIVE)
        }
    }
}
