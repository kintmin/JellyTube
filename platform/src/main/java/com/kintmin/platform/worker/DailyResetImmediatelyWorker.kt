package com.kintmin.platform.worker

import android.content.Context
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.kintmin.domain.step.usecase.BackupStepSensorUseCase
import com.kintmin.domain.step.usecase.DeleteOldStepsUseCase
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter

@HiltWorker
class DailyResetImmediatelyWorker @AssistedInject constructor(
    @Assisted context: Context,
    @Assisted params: WorkerParameters,
    private val backupStepSensorUseCase: BackupStepSensorUseCase,
    private val deleteOldStepsUseCase: DeleteOldStepsUseCase,
) : CoroutineWorker(context, params) {

    companion object {

        const val WORK_NAME = "DailyResetWorkImmediately"
        const val KEY_TARGET_DATE = "KEY_TARGET_DATE"
        const val KEY_LAST_STEPS = "KEY_LAST_STEPS"
        const val KEY_LAST_STEP_SENSOR = "KEY_LAST_STEP_SENSOR"
    }

    override suspend fun doWork(): Result {
        val targetDate = inputData.getString(KEY_TARGET_DATE)
            ?: LocalDate.now().minusDays(1).format(DateTimeFormatter.BASIC_ISO_DATE)
        val lastStepSensor = inputData.getLong(KEY_LAST_STEP_SENSOR, -1L)

        try {
            if (lastStepSensor != -1L) {
                val targetLocalDate = LocalDate.parse(targetDate, DateTimeFormatter.BASIC_ISO_DATE)
                val endOfDayMillis = targetLocalDate
                    .atTime(LocalTime.of(23, 59, 59, 999_000_000))
                    .atZone(ZoneId.systemDefault())
                    .toInstant()
                    .toEpochMilli()

                backupStepSensorUseCase(
                    stepSensor = lastStepSensor,
                    rawCreatedTime = endOfDayMillis,
                )
            }
            deleteOldStepsUseCase()
            return Result.success()
        } catch (_: Exception) {
            return if (runAttemptCount >= 5) {
                Result.failure()
            } else {
                Result.retry()
            }
        }
    }
}