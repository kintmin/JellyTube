package com.kintmin.domain.step

/** 하루가 끝났을 때 그날의 마지막 걸음수를 기록하도록 예약한다. 앱이 꺼져 있어도 남아야 해서 플랫폼이 예약한다. */
interface ScheduleDailyStepClosing {

    operator fun invoke(targetDate: String, lastDailyStep: Int, lastStepSensor: Long?)
}
