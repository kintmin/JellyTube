package com.kintmin.presentation.port

/**
 * 걸음수 측정을 켜고 끈다.
 *
 * 측정에는 권한과 센서가 필요한데 그 판정은 구현체가 한다.
 * 시작할 수 없는 기기에서도 호출 자체는 안전하다.
 */
interface StepMeasurementController {

    fun start()

    fun stop()
}
