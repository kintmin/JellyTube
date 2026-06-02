package com.kintmin.domain.step.usecase

import com.kintmin.domain.step.worker.RegisterDailyResetImmediatelyWorker
import com.kintmin.domain.step.worker.RegisterLoadBalancedDailyResetWorker

class RegisterDailyResetWorkerUseCase constructor(
    private val registerDailyResetImmediatelyWorker: RegisterDailyResetImmediatelyWorker,
    private val registerLoadBalancedDailyResetWorker: RegisterLoadBalancedDailyResetWorker,
) {

    /**
     * ?ì • ë¡œì§?€ ?œë²„ ë¶€?˜ë? ì¤????ˆê¸° ?Œë¬¸??ì¦‰ì‹œ ?µì‹ ??ì¡´ì¬?˜ë©´ ?ˆëœ??
     * ?µì‹  ?„ìš” ??LoadBalancedDailyResetWorker ?ì„œ ë¶„ì‚° ?„ìš”.
     */
    operator fun invoke(targetDate: String, lastDailyStep: Int, lastStepSensor: Long?) {
        registerDailyResetImmediatelyWorker(targetDate, lastDailyStep, lastStepSensor)
        registerLoadBalancedDailyResetWorker(targetDate, lastDailyStep)
    }
}