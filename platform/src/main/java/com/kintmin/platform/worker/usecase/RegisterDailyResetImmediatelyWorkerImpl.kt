package com.kintmin.platform.worker.usecase

import android.content.Context
import androidx.work.ExistingWorkPolicy
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.workDataOf
import com.kintmin.domain.step.worker.RegisterDailyResetImmediatelyWorker
import com.kintmin.platform.worker.DailyResetImmediatelyWorker
import javax.inject.Inject

class RegisterDailyResetImmediatelyWorkerImpl @Inject constructor(
    private val appContext: Context,
): RegisterDailyResetImmediatelyWorker {

    override fun invoke(targetDate: String, lastDailyStep: Int) {
        runCatching {
            val request = OneTimeWorkRequestBuilder<DailyResetImmediatelyWorker>()
                .setInputData(
                    workDataOf(
                        DailyResetImmediatelyWorker.KEY_TARGET_DATE to targetDate,
                        DailyResetImmediatelyWorker.KEY_LAST_STEPS to lastDailyStep,
                    )
                )
                .build()

            WorkManager.getInstance(appContext).enqueueUniqueWork(
                DailyResetImmediatelyWorker.WORK_NAME,
                ExistingWorkPolicy.REPLACE,
                request
            )
        }
    }
}