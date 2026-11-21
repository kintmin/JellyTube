package com.kintmin.jellytube.worker

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.kintmin.domain.backup.usecase.BackupIfDueUseCase
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import kotlin.time.Clock

/** 앱을 열지 않는 날에도 걸음수는 쌓인다. 실패는 다음 주기나 앱이 내려갈 때 다시 시도한다. */
class DailyBackupWorker(
    appContext: Context,
    workerParams: WorkerParameters,
) : CoroutineWorker(appContext, workerParams), KoinComponent {

    private val backupIfDueUseCase: BackupIfDueUseCase by inject()

    override suspend fun doWork(): Result {
        backupIfDueUseCase(Clock.System.now().toEpochMilliseconds())
        return Result.success()
    }
}
