package com.kintmin.jellytube.service_controller

import android.content.Context
import com.kintmin.jellytube.service.StepForegroundService
import com.kintmin.presentation.port.StepMeasurementController

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
