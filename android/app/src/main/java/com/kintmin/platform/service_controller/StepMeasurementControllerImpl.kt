package com.kintmin.platform.service_controller

import android.content.Context
import com.kintmin.platform.service.StepForegroundService
import com.kintmin.presentation.port.StepMeasurementController

/**
 * 걸음수 측정을 foreground service 로 수행한다.
 *
 * 권한·센서 판정과 Result 처리는 [StepForegroundService] 의 companion 이 이미 하고 있어
 * 여기서는 그대로 위임한다.
 */
class StepMeasurementControllerImpl(
    private val appContext: Context,
) : StepMeasurementController {

    override fun start() {
        StepForegroundService.startService(appContext)
    }

    override fun stop() {
        StepForegroundService.stopService(appContext)
    }
}
