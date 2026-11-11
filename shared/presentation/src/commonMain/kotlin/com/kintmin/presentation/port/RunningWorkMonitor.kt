package com.kintmin.presentation.port

import com.kintmin.presentation.port.model.RunningWork

interface RunningWorkMonitor {

    /** 진행 중인 작업만 개수와 함께 담는다. */
    suspend fun runningWorks(): Map<RunningWork, Int>
}
