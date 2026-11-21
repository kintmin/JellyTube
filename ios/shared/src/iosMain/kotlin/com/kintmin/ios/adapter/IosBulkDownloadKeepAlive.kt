package com.kintmin.ios.adapter

import com.kintmin.ios.task.IosRunningTaskTracker
import com.kintmin.presentation.platform.continueInBackgroundWhile
import com.kintmin.presentation.port.BulkDownloadKeepAlive
import com.kintmin.presentation.port.model.BulkDownloadTask

internal class IosBulkDownloadKeepAlive : BulkDownloadKeepAlive {
    override suspend fun <T> keepAlive(task: BulkDownloadTask, block: suspend () -> T): T =
        IosRunningTaskTracker.track(task.runningTask) { continueInBackgroundWhile(block) }
}
