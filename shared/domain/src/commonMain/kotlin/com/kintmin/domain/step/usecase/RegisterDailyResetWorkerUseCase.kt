package com.kintmin.domain.step.usecase

import com.kintmin.domain.step.worker.RegisterDailyResetImmediatelyWorker
import com.kintmin.domain.step.worker.RegisterLoadBalancedDailyResetWorker

class RegisterDailyResetWorkerUseCase constructor(
    private val registerDailyResetImmediatelyWorker: RegisterDailyResetImmediatelyWorker,
    private val registerLoadBalancedDailyResetWorker: RegisterLoadBalancedDailyResetWorker,
) {

    /**
     * 리셋 로직은 서버 부하를 줄이기 위해, 즉시 실행이 존재하면 안된다.
     * 실행 필요 시 LoadBalancedDailyResetWorker 에서 분산 필요.
     */
    operator fun invoke(targetDate: String, lastDailyStep: Int, lastStepSensor: Long?) {
        registerDailyResetImmediatelyWorker(targetDate, lastDailyStep, lastStepSensor)
        registerLoadBalancedDailyResetWorker(targetDate, lastDailyStep)
    }
}
