package com.kintmin.platform.service.sensor

import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener

/**
 * 앱이 foreground일 때 센서 이벤트 감지 필요
 * https://developer.android.com/develop/sensors-and-location/sensors/sensors_overview?hl=ko#only-gather-sensor-data-in-the-foreground
 *
 * 걸음수 문서 참고
 * https://developer.android.com/health-and-fitness/fitness/basic-app/read-step-count-data
 */
class StepCounterSensorListener(
    private val updateStep: (Long) -> Unit,
) : SensorEventListener {

    override fun onSensorChanged(sensorEvent: SensorEvent?) {
        if (sensorEvent == null) return

        val stepsSinceLastReboot = sensorEvent.values.firstOrNull()?.toLong() ?: return
        updateStep(stepsSinceLastReboot)
    }

    override fun onAccuracyChanged(p0: Sensor?, p1: Int) {}
}