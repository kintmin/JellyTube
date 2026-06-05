package com.kintmin.platform.worker

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.kintmin.domain.extension.toBasicIsoString
import com.kintmin.domain.step.repository.StepRepository
import kotlinx.datetime.Clock
import kotlinx.datetime.DateTimeUnit
import kotlinx.datetime.TimeZone
import kotlinx.datetime.minus
import kotlinx.datetime.toLocalDateTime
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject

class LoadBalancedDailyResetWorker(
    context: Context,
    params: WorkerParameters,
) : CoroutineWorker(context, params), KoinComponent {

    private val stepRepository: StepRepository by inject()

    companion object {

        const val WORK_NAME = "DailyResetWork"
        const val KEY_LAST_STEPS = "KEY_LAST_STEPS"
        const val KEY_TARGET_DATE = "KEY_TARGET_DATE"
    }

    override suspend fun doWork(): Result {
        val targetDate = inputData.getString(KEY_TARGET_DATE)
            ?: Clock.System.now().toLocalDateTime(TimeZone.currentSystemDefault()).date
                .minus(1, DateTimeUnit.DAY).toBasicIsoString()
        val lastStep = inputData.getInt(KEY_LAST_STEPS, 0)

        try {
            // 서버 분산
            return Result.success()
        } catch (e: Exception) {
            return Result.failure()
        }
    }
}
