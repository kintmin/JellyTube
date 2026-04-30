package com.kintmin.domain.step.worker

interface RegisterDailyResetImmediatelyWorker {

    operator fun invoke(targetDate: String, lastDailyStep: Int)
}
