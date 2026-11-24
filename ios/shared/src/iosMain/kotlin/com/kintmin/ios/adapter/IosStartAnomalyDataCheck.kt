package com.kintmin.ios.adapter

import com.kintmin.domain.audio_media.usecase.CleanupAnomalousDataUseCase
import com.kintmin.ios.notification.IosLocalNotification
import com.kintmin.ios.task.IosRunningTaskTracker
import com.kintmin.log.AppLog
import com.kintmin.log.model.DebugLog
import com.kintmin.presentation.port.StartAnomalyDataCheck
import com.kintmin.presentation.port.model.RunningTask
import com.kintmin.presentation.port.model.WorkResultNotice
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex

/**
 * 전송이 아니라 계산 작업이라 background `URLSession` 과 무관하다. 앱 수명 스코프에서 돌고
 * 끝나면 로컬 알림으로 알린다 — **앱이 내려가면 중단된다.**
 */
internal class IosStartAnomalyDataCheck(
    private val cleanupAnomalousDataUseCase: CleanupAnomalousDataUseCase,
    private val appLog: AppLog,
) : StartAnomalyDataCheck {

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main)

    /**
     * `tryLock` 인 것은 기다리지 않고 "이미 돌고 있음"을 false 로 즉시 돌려주는 계약이라서다.
     * 푸는 것은 `finally` 다 — 한 번 못 풀면 앱이 죽을 때까지 기능이 잠긴다.
     */
    private val runGate = Mutex()

    override suspend fun invoke(): Boolean {
        if (!runGate.tryLock()) return false

        scope.launch {
            try {
                IosRunningTaskTracker.track(RunningTask.ANOMALY_DATA_CHECK) { cleanupAnomalousDataUseCase() }
                    .onSuccess { anomalyCount ->
                        IosLocalNotification.show(WorkResultNotice.AnomalyCheck.Success(anomalyCount))
                    }
                    .onFailure {
                        appLog.sendDebugLog(DebugLog("IosStartAnomalyDataCheck", "이상 데이터 점검 실패"))
                        IosLocalNotification.show(WorkResultNotice.AnomalyCheck.Failure)
                    }
            } finally {
                runGate.unlock()
            }
        }
        return true
    }
}
