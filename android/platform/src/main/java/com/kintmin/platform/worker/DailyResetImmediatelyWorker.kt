package com.kintmin.platform.worker

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.kintmin.domain.extension.parseBasicIsoDate
import com.kintmin.domain.extension.startOfDayMillis
import com.kintmin.domain.extension.toBasicIsoString
import com.kintmin.domain.step.usecase.BackupStepSensorUseCase
import kotlinx.datetime.Clock
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

        try {
            if (lastStepSensor != -1L) {
                val targetLocalDate = targetDate.parseBasicIsoDate()
                val endOfDayMillis = targetLocalDate
                    .plus(1, DateTimeUnit.DAY)
                    .startOfDayMillis(timeZone)

                backupStepSensorUseCase(
                    stepSensor = lastStepSensor,
                    rawCreatedTime = endOfDayMillis,
                )
            }
            return Result.success()
        } catch (_: Exception) {
            return if (runAttemptCount >= 3) {
                Result.failure()
            } else {
                Result.retry()
            }
        }
    }
}
