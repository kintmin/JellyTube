package com.kintmin.presentation.port

import com.kintmin.domain.audio_media.usecase.CleanupAnomalousDataUseCase
import com.kintmin.log.AppLog
import com.kintmin.log.model.DebugLog
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex

/**
 * 이상 데이터 점검.
 *
 * **전송이 아니라 계산 작업이라 background `URLSession` 과 무관하다.** 앱이 떠 있는 동안
 * 앱 수명 스코프에서 돌고, 끝나면 로컬 알림으로 알린다. Android 는 WorkManager 가 같은 일을
 * 하면서 앱이 내려가도 이어 주지만, iOS 에는 그런 장치가 없다 — 앱이 내려가면 중단된다.
 * 사용자가 설정 화면에서 직접 누르는 작업이라 그 사이 앱을 내릴 이유가 적다.
 */
internal class IosExecuteAnomalyDataCheck(
    private val cleanupAnomalousDataUseCase: CleanupAnomalousDataUseCase,
    private val appLog: AppLog,
) : ExecuteAnomalyDataCheck {

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main)

    /**
     * Android 의 `enqueueUniqueWork(KEEP)` 자리. 두 번 눌러도 두 번 돌지 않게 막는다.
     * `Mutex` 를 쓰는 것은 **기다리기 위해서가 아니라** `tryLock` 으로 "이미 돌고 있음"을
     * 즉시 가려내기 위해서다 — 호출 계약이 "기다림" 이 아니라 "false" 다.
     */
    private val runGate = Mutex()

    override suspend fun invoke(): Boolean {
        if (!runGate.tryLock()) return false

        scope.launch {
            cleanupAnomalousDataUseCase()
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
                    )
                }
            runGate.unlock()
        }
        return true
    }
}
