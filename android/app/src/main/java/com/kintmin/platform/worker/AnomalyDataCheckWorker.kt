package com.kintmin.platform.worker

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.kintmin.domain.audio_media.usecase.CleanupAnomalousDataUseCase
import com.kintmin.log.AppLog
import com.kintmin.log.model.DebugLog
import com.kintmin.platform.push_notification.PushNotificationManager
import com.kintmin.platform.push_notification.notifications.AnomalyCheckResultNotification
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject

class AnomalyDataCheckWorker(
    appContext: Context,
    workerParams: WorkerParameters,
) : CoroutineWorker(appContext, workerParams), KoinComponent {

    private val cleanupAnomalousDataUseCase: CleanupAnomalousDataUseCase by inject()
    private val pushNotificationManager: PushNotificationManager by inject()
    private val appLog: AppLog by inject()

    override suspend fun doWork(): Result {
        cleanupAnomalousDataUseCase()
            .onSuccess { anomalyCount ->
                pushNotificationManager.sendNotification(
                    AnomalyCheckResultNotification(
                        resultType = AnomalyCheckResultNotification.ResultType.Success,
                        anomalyCount = anomalyCount,
                    )
                )
                return Result.success()
            }
            .onFailure {
                appLog.sendDebugLog(DebugLog("AnomalyDataCheckWorker", it.message ?: "알 수 없는 오류"))
                pushNotificationManager.sendNotification(
                    AnomalyCheckResultNotification(
                        resultType = AnomalyCheckResultNotification.ResultType.Failure,
                    )
                )
            }
        return Result.failure()
    }
}
