package com.kintmin.domain.file_share.repository

import com.kintmin.domain.file_share.PcUploadHandler
import kotlinx.coroutines.flow.StateFlow

/** PC 업로드를 받는 서버와 그 광고. 앱이 서버를 직접 띄우는 플랫폼(iOS)에만 있다. */
interface PcFileShareServerRepository {
    val isRunning: StateFlow<Boolean>

    fun start(handler: PcUploadHandler)

    fun stop()
}
