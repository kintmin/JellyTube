package com.kintmin.presentation.port

/** 권한·센서 판정은 구현이 한다. 시작할 수 없는 기기에서도 호출은 안전하다. */
interface StepMeasurementController {

    fun start()

    fun stop()
}
