package com.kintmin.domain.step.usecase

import com.kintmin.domain.step.worker.RegisterDailyResetImmediatelyWorker
import com.kintmin.domain.step.worker.RegisterLoadBalancedDailyResetWorker
import javax.inject.Inject

class RegisterDailyResetWorkerUseCase @Inject constructor(
    private val registerDailyResetImmediatelyWorker: RegisterDailyResetImmediatelyWorker,
    private val registerLoadBalancedDailyResetWorker: RegisterLoadBalancedDailyResetWorker,
) {

    /**
     * 자정 로직은 서버 부하를 줄 수 있기 때문에 즉시 통신이 존재하면 안된다.
     * 통신 필요 시 LoadBalancedDailyResetWorker 에서 분산 필요.
     */
    operator fun invoke(targetDate: String, lastDailyStep: Int, lastStepSensor: Long?) {
        registerDailyResetImmediatelyWorker(targetDate, lastDailyStep, lastStepSensor)
        registerLoadBalancedDailyResetWorker(targetDate, lastDailyStep)
    }
}