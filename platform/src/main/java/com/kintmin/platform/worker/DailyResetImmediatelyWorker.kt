package com.kintmin.platform.worker

import android.content.Context
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.kintmin.domain.step.usecase.DeleteOldStepsUseCase
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import java.time.LocalDate
import java.time.format.DateTimeFormatter

@HiltWorker
class DailyResetImmediatelyWorker @AssistedInject constructor(
    @Assisted context: Context,
    @Assisted params: WorkerParameters,
    private val deleteOldStepsUseCase: DeleteOldStepsUseCase,
) : CoroutineWorker(context, params) {

    companion object {

        const val WORK_NAME = "DailyResetWorkImmediately"
        const val KEY_LAST_STEPS = "KEY_LAST_STEPS"
        const val KEY_TARGET_DATE = "KEY_TARGET_DATE"
    }

    override suspend fun doWork(): Result {
        val targetDate = inputData.getString(KEY_TARGET_DATE) ?: LocalDate.now().minusDays(1).format(DateTimeFormatter.ofPattern("yyyyMMdd"))
        val lastStep = inputData.getInt(KEY_LAST_STEPS, 0)

        try {
            deleteOldStepsUseCase()
            return Result.success()
        } catch (e: Exception) {
            return if (runAttemptCount >= 5) {
                Result.failure()
            } else {
                Result.retry()
            }
        }
    }
}