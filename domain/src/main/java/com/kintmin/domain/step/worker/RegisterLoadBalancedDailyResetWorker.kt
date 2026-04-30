package com.kintmin.domain.step.worker

interface RegisterLoadBalancedDailyResetWorker {

    operator fun invoke(targetDate: String, lastDailyStep: Int)
}
