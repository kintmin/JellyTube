package com.kintmin.platform.worker.usecase

import android.content.Context
import androidx.work.ExistingWorkPolicy
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import com.kintmin.platform.worker.AnomalyDataCheckWorker
import kotlinx.coroutines.flow.first

class ExecuteAnomalyDataCheck(
    private val appContext: Context,
) {

    suspend operator fun invoke(): Boolean {
        val workManager = WorkManager.getInstance(appContext)

        val isAlreadyRunning = workManager.getWorkInfosForUniqueWorkFlow(WORK_NAME).first()
            .any { !it.state.isFinished }
        if (isAlreadyRunning) return false

        val request = OneTimeWorkRequestBuilder<AnomalyDataCheckWorker>().build()
        workManager.enqueueUniqueWork(WORK_NAME, ExistingWorkPolicy.KEEP, request)
        return true
    }

    companion object {
        const val WORK_NAME = "anomaly_data_check"
    }
}
