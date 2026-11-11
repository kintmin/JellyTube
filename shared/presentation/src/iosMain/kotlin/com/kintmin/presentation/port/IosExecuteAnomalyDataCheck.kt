package com.kintmin.presentation.port

import com.kintmin.domain.audio_media.usecase.CleanupAnomalousDataUseCase
import com.kintmin.log.AppLog
import com.kintmin.log.model.DebugLog
import com.kintmin.presentation.deeplink.DeepLinkConstants
import com.kintmin.presentation.port.model.RunningWork
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex

/**
 * 이상 데이터 점검.
 *
 * 전송이 아니라 계산 작업이라 background `URLSession` 과 무관하다. 앱 수명 스코프에서 돌고
 * 끝나면 로컬 알림으로 알린다. Android 의 WorkManager 와 달리 **앱이 내려가면 중단된다.**
 */
internal class IosExecuteAnomalyDataCheck(
    private val cleanupAnomalousDataUseCase: CleanupAnomalousDataUseCase,
    private val appLog: AppLog,
) : ExecuteAnomalyDataCheck {

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main)

    /**
     * Android 의 `enqueueUniqueWork(KEEP)` 자리. 두 번 눌러도 두 번 돌지 않게 막는다.
     * `tryLock` 인 것은 기다리지 않고 "이미 돌고 있음"을 false 로 즉시 돌려주는 계약이라서다.
     * 푸는 것은 `finally` 다 — 한 번 못 풀면 앱이 죽을 때까지 기능이 잠긴다.
     */
    private val runGate = Mutex()

    override suspend fun invoke(): Boolean {
        if (!runGate.tryLock()) return false

        scope.launch {
            try {
                IosRunningWorkTracker.track(RunningWork.ANOMALY_DATA_CHECK) { cleanupAnomalousDataUseCase() }
                    .onSuccess { anomalyCount ->
                        IosLocalNotification.show(
                            title = "이상 데이터 점검 완료",
                            body = "이상 데이터 점검에 완료했습니다.\n발견된 이상 데이터: ${anomalyCount}건",
                        )
                    }
                    .onFailure { throwable ->
                        appLog.sendDebugLog(
                            DebugLog("IosExecuteAnomalyDataCheck", throwable.message ?: "알 수 없는 오류"),
                        )
                        IosLocalNotification.show(
                            title = "이상 데이터 점검 실패",
                            body = "작업 도중 오류가 발생했습니다\n앱 로그를 확인해주세요.",
                            deepLink = DeepLinkConstants.UrlBuilder.appLogScreen(),
                        )
                    }
            } finally {
                runGate.unlock()
            }
        }
        return true
    }
}
