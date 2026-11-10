package com.kintmin.ios.step

import com.kintmin.domain.step.worker.RegisterDailyResetImmediatelyWorker
import com.kintmin.domain.step.worker.RegisterLoadBalancedDailyResetWorker

/**
 * 일일 리셋 예약 포트의 iOS 스텁. **예약할 일이 없다** — 자정을 넘기는 처리는
 * `IosStepMeasurementController` 가 직접 한다. 도메인 포트라 구현은 조립 모듈에 둔다.
 */
internal class IosRegisterDailyResetImmediatelyWorker : RegisterDailyResetImmediatelyWorker {

    override fun invoke(targetDate: String, lastDailyStep: Int, lastStepSensor: Long?) = Unit
}

internal class IosRegisterLoadBalancedDailyResetWorker : RegisterLoadBalancedDailyResetWorker {

    override fun invoke(targetDate: String, lastDailyStep: Int) = Unit
}
