package com.kintmin.platform.service

import android.app.Service
import android.content.ComponentCallbacks
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.res.Configuration
import android.hardware.Sensor
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.os.IBinder
import com.kintmin.domain.step.usecase.BackupStepSensorUseCase
import com.kintmin.domain.step.usecase.GetAccelerateStepUseCase
import com.kintmin.domain.step.usecase.GetLastStepSensorUseCase
import com.kintmin.domain.step.usecase.GetTodayStepCountUseCase
import com.kintmin.domain.step.usecase.ResetDataOncePerDayUseCase
import com.kintmin.domain.step.usecase.UpdateAccelerateStepUseCase
import com.kintmin.domain.step.usecase.UpdateLastStepSensorUseCase
import com.kintmin.domain.step.usecase.UpdateTodayStepCountUseCase
import com.kintmin.platform.notification.NotificationData
import com.kintmin.platform.notification.PushNotificationUtil
import com.kintmin.platform.receiver.DateChangeBroadcastReceiver
import com.kintmin.platform.service.sensor.AccelerometerSensorListener
import com.kintmin.platform.service.sensor.StepCounterSensorListener
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancelChildren
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.merge
import kotlinx.coroutines.flow.sample
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@OptIn(FlowPreview::class)
@AndroidEntryPoint
class StepForegroundService : Service() {

    @Inject lateinit var notificationUtil: PushNotificationUtil
    @Inject lateinit var resetDataOncePerDayUseCase: ResetDataOncePerDayUseCase

    @Inject lateinit var getTodayStepCountUseCase: GetTodayStepCountUseCase
    @Inject lateinit var getLastStepSensorUseCase: GetLastStepSensorUseCase
    @Inject lateinit var getAccelerateStepUseCase: GetAccelerateStepUseCase

    @Inject lateinit var updateTodayStepCountUseCase: UpdateTodayStepCountUseCase
    @Inject lateinit var updateLastStepSensorUseCase: UpdateLastStepSensorUseCase
    @Inject lateinit var updateAccelerateStepUseCase: UpdateAccelerateStepUseCase

    @Inject lateinit var backupStepSensorUseCase: BackupStepSensorUseCase

    private val foregroundServiceScope = CoroutineScope(Dispatchers.Main.immediate + SupervisorJob())

    private var stepSensorListener: SensorEventListener? = null
    private var accelerometerSensorListener: SensorEventListener? = null

    private val currentStep = MutableStateFlow(0)
    private val currentStepSensor = MutableStateFlow<Long?>(null)
    private val currentAccelerateStep = MutableStateFlow(0)

    private val dateChangeBroadcastReceiver = DateChangeBroadcastReceiver().apply {
        setOnDateChangedListener(object : DateChangeBroadcastReceiver.OnDateChangedListener {
            override fun onDateChanged() {
                checkDailyReset()
            }

            override fun onTimeChanged() {
            }

            override fun onTimezoneChanged(timeZone: String) {
                // TODO: currentStep 재조정 필요
            }
        })
    }

    private val lightDarkModeSwitchListener = object : ComponentCallbacks {
        override fun onConfigurationChanged(newConfig: Configuration) {
            updateForegroundNotification()
        }

        override fun onLowMemory() {}
    }

    override fun onBind(p0: Intent?): IBinder? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        notificationUtil.startForeground(this, NotificationData.SensorStep(0, 0))
        return START_STICKY
    }

    override fun onCreate() {
        super.onCreate()

        foregroundServiceScope.launch {
            val todayStepCount = getTodayStepCountUseCase().getOrDefault(0)
            currentStep.update { todayStepCount }

            val lastStepSensor = getLastStepSensorUseCase().getOrNull()
            currentStepSensor.update { lastStepSensor }

            val todayAccelerateStepCount = getAccelerateStepUseCase().getOrDefault(0)
            currentAccelerateStep.update { todayAccelerateStepCount }

            registerStepSensor().mapCatching {
                registerAccelerometerSensor().getOrThrow()
            }.onSuccess {
                registerStepResetReceiver()
                registerLightDarkModeSwitchListener()
                observeNotificationData()
                registerStepBackup()
                // TODO: 30분마다 foreground 꺼졌는지 체크하는 background (필수는 아님)
            }.onFailure {

                print(it)
            }
        }
    }

    override fun onDestroy() {
        unregisterSensor()
        unregisterStepResetReceiver()
        unregisterLightDarkModeSwitchListener()
        foregroundServiceScope.coroutineContext.cancelChildren()

        super.onDestroy()
    }

    private fun registerStepSensor(): Result<Unit> = runCatching {
        val sensorManager = getSystemService(Context.SENSOR_SERVICE) as SensorManager
        val stepSensor = sensorManager.getDefaultSensor(Sensor.TYPE_STEP_COUNTER)!!

        stepSensorListener = StepCounterSensorListener(
            updateStep = { stepsSinceLastReboot -> updateStep(stepsSinceLastReboot) },
            checkDailyReset = { checkDailyReset() },
        )
        sensorManager.registerListener(stepSensorListener, stepSensor, SensorManager.SENSOR_DELAY_UI)
    }

    private fun registerAccelerometerSensor(): Result<Unit> = runCatching {
        val sensorManager = getSystemService(Context.SENSOR_SERVICE) as SensorManager
        val accelerometerSensor = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)!!

        accelerometerSensorListener = AccelerometerSensorListener(
            updateStep = { updateAccelerateStep() },
            checkDailyReset = { checkDailyReset() },
        )
        sensorManager.registerListener(accelerometerSensorListener, accelerometerSensor, SensorManager.SENSOR_DELAY_GAME)
    }

    private fun registerStepResetReceiver() {
        runCatching {
            val filter = IntentFilter().apply {
                addAction(Intent.ACTION_DATE_CHANGED)
                addAction(Intent.ACTION_TIME_CHANGED)
                addAction(Intent.ACTION_TIMEZONE_CHANGED)
            }
            registerReceiver(dateChangeBroadcastReceiver, filter)
        }
    }

    private fun registerLightDarkModeSwitchListener() {
        registerComponentCallbacks(lightDarkModeSwitchListener)
    }

    private fun unregisterSensor() {
        runCatching {
            val sensorManager = getSystemService(Context.SENSOR_SERVICE) as SensorManager

            stepSensorListener?.let { sensorManager.unregisterListener(it) }
            stepSensorListener = null

            accelerometerSensorListener?.let { sensorManager.unregisterListener(it) }
            accelerometerSensorListener = null
        }
    }

    private fun unregisterStepResetReceiver() {
        runCatching { unregisterReceiver(dateChangeBroadcastReceiver) }
    }

    private fun unregisterLightDarkModeSwitchListener() {
        runCatching { unregisterComponentCallbacks(lightDarkModeSwitchListener) }
    }

    private fun observeNotificationData() {
        foregroundServiceScope.launch {
            combine(
                currentStep,
                currentAccelerateStep,
            ) { _, _ -> }
                .sample(1000L)
                .collect {
                    updateForegroundNotification()
                }
        }
    }

    private fun registerStepBackup() {
        // datastore는 누락없이 즉시 update
        foregroundServiceScope.launch {
            currentStep.collect {
                updateTodayStepCountUseCase(it)
            }
        }
        foregroundServiceScope.launch {
            currentStepSensor.filterNotNull().collect {
                updateLastStepSensorUseCase(it)
            }
        }
        foregroundServiceScope.launch {
            currentAccelerateStep.collect {
                updateAccelerateStepUseCase(it)
            }
        }

        // roomDB는 1분 단위로 update
        foregroundServiceScope.launch {
            currentStepSensor.filterNotNull().map { stepSensor ->
                val now = System.currentTimeMillis()
                val minuteTime = now - (now % 60_000L)

                minuteTime to stepSensor
            }.distinctUntilChanged { old, new ->
                old.first == new.first
            }.collect { (_, stepSensor) ->
                backupStepSensorUseCase(stepSensor)
            }
        }
    }

    private fun updateForegroundNotification() {
        runCatching {
            notificationUtil.sendNotification(NotificationData.SensorStep(currentStep.value, currentAccelerateStep.value))
        }
    }

    private fun checkDailyReset() {
        resetDataOncePerDayUseCase.invoke(currentStep.value, foregroundServiceScope) {
            currentStep.update { 0 }
            currentAccelerateStep.update { 0 }
        }
    }

    private fun updateStep(newStepSensor: Long) {
        val prevStepSensor = currentStepSensor.value

        if (prevStepSensor != null && newStepSensor > prevStepSensor) {
            val stepDelta = (newStepSensor - prevStepSensor).toInt()
            currentStep.update { prevStep -> prevStep + stepDelta }
        }

        currentStepSensor.update { newStepSensor }
    }

    private fun updateAccelerateStep() {
        currentAccelerateStep.update { it + 1 }
    }
}