package com.kintmin.ios.step

import com.kintmin.domain.step.worker.RegisterDailyResetImmediatelyWorker
import com.kintmin.domain.step.worker.RegisterLoadBalancedDailyResetWorker

/**
 * 일일 리셋 예약 포트의 iOS 스텁. **예약할 일이 없다.**
 *
 * Android 는 센서값이 부팅 누적이라 자정에 기준을 끊어 줄 워커가 필요하다. iOS 는
 * `CMPedometer` 가 하루 기준 누적을 소급 조회해 주고, `IosStepMeasurementController` 가
 * 자정을 넘길 때 직접 어제를 마감하고 다시 건다.
 *
 * 도메인 포트라 구현을 `shared:domain` 에 두지 않고 조립 모듈에 둔다.
 */
internal class IosRegisterDailyResetImmediatelyWorker : RegisterDailyResetImmediatelyWorker {

    override fun invoke(targetDate: String, lastDailyStep: Int, lastStepSensor: Long?) = Unit
}

internal class IosRegisterLoadBalancedDailyResetWorker : RegisterLoadBalancedDailyResetWorker {

    override fun invoke(targetDate: String, lastDailyStep: Int) = Unit
}
