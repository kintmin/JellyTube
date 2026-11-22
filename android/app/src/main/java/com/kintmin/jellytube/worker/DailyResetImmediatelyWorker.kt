package com.kintmin.jellytube.worker

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.ListenableWorker
import androidx.work.WorkerParameters
import com.kintmin.domain.extension.parseBasicIsoDate
import com.kintmin.domain.extension.startOfDayMillis
import com.kintmin.domain.extension.toBasicIsoString
import com.kintmin.domain.step.usecase.BackupStepSensorUseCase
import kotlin.time.Clock
import kotlinx.datetime.DateTimeUnit
import kotlinx.datetime.TimeZone
import kotlinx.datetime.minus
import kotlinx.datetime.plus
import kotlinx.datetime.toLocalDateTime
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject

class DailyResetImmediatelyWorker(
    context: Context,
    params: WorkerParameters,
) : CoroutineWorker(context, params), KoinComponent {

    private val backupStepSensorUseCase: BackupStepSensorUseCase by inject()

    companion object {

        const val WORK_NAME = "DailyResetWorkImmediately"
        const val KEY_TARGET_DATE = "KEY_TARGET_DATE"
        const val KEY_LAST_STEPS = "KEY_LAST_STEPS"
        const val KEY_LAST_STEP_SENSOR = "KEY_LAST_STEP_SENSOR"
    }

    override suspend fun doWork(): Result {
        val timeZone = TimeZone.currentSystemDefault()
        val targetDate = inputData.getString(KEY_TARGET_DATE)
            ?: Clock.System.now().toLocalDateTime(timeZone).date
                .minus(1, DateTimeUnit.DAY).toBasicIsoString()
        val lastStepSensor = inputData.getLong(KEY_LAST_STEP_SENSOR, -1L)

        if (lastStepSensor == -1L) return Result.success()

        val endOfDayMillis = targetDate.parseBasicIsoDate()
            .plus(1, DateTimeUnit.DAY)
            .startOfDayMillis(timeZone)
        val saved = backupStepSensorUseCase(stepSensor = lastStepSensor, rawCreatedTime = endOfDayMillis)
        return dailyResetWorkResult(saved, runAttemptCount)
    }
}

internal fun dailyResetWorkResult(saved: kotlin.Result<Unit>, runAttemptCount: Int): ListenableWorker.Result = when {
    saved.isSuccess -> ListenableWorker.Result.success()
    runAttemptCount >= 3 -> ListenableWorker.Result.failure()
    else -> ListenableWorker.Result.retry()
}
