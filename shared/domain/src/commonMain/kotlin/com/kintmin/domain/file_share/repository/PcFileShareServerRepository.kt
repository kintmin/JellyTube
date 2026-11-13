package com.kintmin.domain.file_share.repository

import com.kintmin.domain.file_share.PcUploadHandler
import kotlinx.coroutines.flow.StateFlow

/** PC 업로드를 받는 서버와 그 광고. */
interface PcFileShareServerRepository {
    val isRunning: StateFlow<Boolean>

    fun start(handler: PcUploadHandler)

    fun stop()
}
