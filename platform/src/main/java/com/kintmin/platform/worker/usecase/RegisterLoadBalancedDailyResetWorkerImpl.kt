package com.kintmin.platform.worker.usecase

import android.content.Context
import androidx.work.Constraints
import androidx.work.ExistingWorkPolicy
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.workDataOf
import com.kintmin.domain.step.usecase.FetchLoadBalancingDelaySecondUseCase
import com.kintmin.domain.step.worker.RegisterLoadBalancedDailyResetWorker
import com.kintmin.platform.worker.LoadBalancedDailyResetWorker
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import kotlin.random.Random

class RegisterLoadBalancedDailyResetWorkerImpl @Inject constructor(
    private val appContext: Context,
    private val fetchLoadBalancingDelaySecondUseCase: FetchLoadBalancingDelaySecondUseCase,
): RegisterLoadBalancedDailyResetWorker {

    override fun invoke(targetDate: String, lastDailyStep: Int) {
        runCatching {
            val within30Min = 60 * 30L
            val random = Random(seed = System.currentTimeMillis())
            val loadBalancingDelayInSeconds = fetchLoadBalancingDelaySecondUseCase(userCode = "", random = random, maxSecond = within30Min)

            val request = OneTimeWorkRequestBuilder<LoadBalancedDailyResetWorker>()
                .setInputData(workDataOf(LoadBalancedDailyResetWorker.KEY_LAST_STEPS to lastDailyStep))
                .setInitialDelay(loadBalancingDelayInSeconds, TimeUnit.SECONDS)
                .setConstraints(Constraints.Builder().setRequiredNetworkType(NetworkType.CONNECTED).build())
                .build()

            WorkManager.getInstance(appContext).enqueueUniqueWork(
                LoadBalancedDailyResetWorker.WORK_NAME,
                ExistingWorkPolicy.REPLACE,
                request
            )
        }
    }
}