package com.kintmin.jellytube.worker

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.kintmin.domain.audio_media.usecase.CleanupAnomalousDataUseCase
import com.kintmin.log.AppLog
import com.kintmin.log.model.DebugLog
import com.kintmin.jellytube.push_notification.PushNotificationManager
import com.kintmin.jellytube.push_notification.notifications.AnomalyCheckResultNotification
import com.kintmin.presentation.port.model.WorkResultNotice
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
                    AnomalyCheckResultNotification(WorkResultNotice.AnomalyCheck.Success(anomalyCount)),
                )
                return Result.success()
            }
            .onFailure {
                appLog.sendDebugLog(DebugLog("AnomalyDataCheckWorker", "이상 데이터 점검 실패"))
                pushNotificationManager.sendNotification(
                    AnomalyCheckResultNotification(WorkResultNotice.AnomalyCheck.Failure),
                )
            }
        return Result.failure()
    }
}
