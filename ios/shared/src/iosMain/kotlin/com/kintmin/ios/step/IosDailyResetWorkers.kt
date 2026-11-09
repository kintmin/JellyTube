package com.kintmin.ios.step

import com.kintmin.domain.step.worker.RegisterDailyResetImmediatelyWorker
import com.kintmin.domain.step.worker.RegisterLoadBalancedDailyResetWorker

/**
 * 일일 리셋 예약 포트의 iOS 스텁.
 *
 * Android 는 WorkManager 로 자정 리셋을 예약한다. iOS 에는 대응물이 없고, 필요도 없다 —
 * `CMPedometer` 가 최대 7일치를 소급 조회해 주므로 **앱 포그라운드 진입 때
 * `ResetDataOncePerDayUseCase` 를 직접 부르는 쪽**이 맞다. 걸음수 측정을 붙일 때 그 호출을
 * 진입점에 넣고 이 스텁은 "예약 불필요" 로 확정된다.
 *
 * 도메인 포트라 구현을 `shared:domain` 에 두지 않고 조립 모듈에 둔다.
 */
internal class IosRegisterDailyResetImmediatelyWorker : RegisterDailyResetImmediatelyWorker {

    override fun invoke(targetDate: String, lastDailyStep: Int, lastStepSensor: Long?) = Unit
}

internal class IosRegisterLoadBalancedDailyResetWorker : RegisterLoadBalancedDailyResetWorker {

    override fun invoke(targetDate: String, lastDailyStep: Int) = Unit
}
