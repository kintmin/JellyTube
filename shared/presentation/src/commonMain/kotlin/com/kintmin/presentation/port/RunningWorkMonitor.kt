package com.kintmin.presentation.port

import com.kintmin.presentation.port.model.RunningWork

/** 지금 돌고 있는 백그라운드 작업. 데이터 이전은 이게 비어 있을 때만 시작한다. */
interface RunningWorkMonitor {

    /** 작업 종류 → 진행 중인 개수. 진행 중인 것만 담는다. */
    suspend fun runningWorks(): Map<RunningWork, Int>
}
