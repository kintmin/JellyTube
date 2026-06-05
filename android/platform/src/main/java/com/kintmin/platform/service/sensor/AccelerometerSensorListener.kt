package com.kintmin.platform.service.sensor

import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import kotlin.math.abs

/**
 * 앱이 foreground일 때 센서 이벤트 감지 필요
 * 참고: https://developer.android.com/develop/sensors-and-location/sensors/sensors_overview?hl=ko#only-gather-sensor-data-in-the-foreground
 *
 * 가속도 알고리즘 참고
 * 공식 레포는 아니지만, fork 및 star 수가 많은 레거시 센서 알고리즘.
 * https://github.com/bagilevi/android-pedometer/blob/master/src/name/bagi/levente/pedometer/StepDetector.java
 */
class AccelerometerSensorListener(
    private val updateStep: () -> Unit,
) : SensorEventListener {

    private var yOffset: Float = 480 * 0.5f
    private var lastMatch = -1
    private var lastSensorMillis: Long = 0

    private val limit = PREFERENCE_ACCELEROMETER_THRESHOLD.toFloat()
    private val lastValues = FloatArray(3 * 2)
    private val scale = floatArrayOf(
        -(480 * 0.5f * (1.0f / (SensorManager.STANDARD_GRAVITY * 2))),
        -(480 * 0.5f * (1.0f / SensorManager.MAGNETIC_FIELD_EARTH_MAX))
    )
    private val lastDirections = FloatArray(3 * 2)
    private val lastExtremes = arrayOf(FloatArray(3 * 2), FloatArray(3 * 2))
    private val lastDiff = FloatArray(3 * 2)

    override fun onSensorChanged(sensorEvent: SensorEvent?) {
        if (sensorEvent == null) return

        var vSum = 0f
        (0..2).forEach {
            val v = yOffset + sensorEvent.values[it] * scale[1]
            vSum += v
        }
        val k = 0
        val v = vSum / 3

        val direction =
            (if (v > lastValues[k]) 1 else if (v < lastValues[k]) -1 else 0).toFloat()
        if (direction == -lastDirections[k]) {
            val extType = if (direction > 0) 0 else 1
            lastExtremes[extType][k] = lastValues[k]
            val diff = abs(lastExtremes[extType][k] - lastExtremes[1 - extType][k])
            if (diff > limit) {
                val isAlmostAsLargeAsPrevious = diff > lastDiff[k] * 2 / 3
                val isPreviousLargeEnough = lastDiff[k] > diff / 3
                val isNotContra = lastMatch != 1 - extType
                if (isAlmostAsLargeAsPrevious && isPreviousLargeEnough && isNotContra) {
                    val currentMillis = System.currentTimeMillis()
                    if (currentMillis - lastSensorMillis > PREFERENCE_ACCELEROMETER_STEP_DELAY) {
                        lastSensorMillis = currentMillis
                        updateStep()
                    }
                    lastMatch = extType
                } else {
                    lastMatch = -1
                }
            }
            lastDiff[k] = diff
        }
        lastDirections[k] = direction
        lastValues[k] = v
    }

    override fun onAccuracyChanged(p0: Sensor?, p1: Int) {}

    private companion object {

        private const val PREFERENCE_ACCELEROMETER_THRESHOLD = 6
        private const val PREFERENCE_ACCELEROMETER_STEP_DELAY = 300
    }
}