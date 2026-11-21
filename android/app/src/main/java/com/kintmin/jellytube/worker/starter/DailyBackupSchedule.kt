package com.kintmin.jellytube.worker.usecase

import android.content.Context
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import com.kintmin.jellytube.worker.DailyBackupWorker
import java.util.concurrent.TimeUnit

object DailyBackupSchedule {

    const val WORK_NAME = "daily_backup"

    /** 이미 예약돼 있으면 그대로 둔다. 바꿔 끼우면 주기를 처음부터 다시 센다. */
    fun schedule(appContext: Context) {
        runCatching {
            WorkManager.getInstance(appContext).enqueueUniquePeriodicWork(
                WORK_NAME,
                ExistingPeriodicWorkPolicy.KEEP,
                PeriodicWorkRequestBuilder<DailyBackupWorker>(1, TimeUnit.DAYS).build(),
            )
        }
    }
}
