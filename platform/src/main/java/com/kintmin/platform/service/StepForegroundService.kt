package com.kintmin.platform.service

import android.Manifest.permission
import android.annotation.SuppressLint
import android.app.Service
import android.content.ComponentCallbacks
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageManager.PERMISSION_GRANTED
import android.content.pm.ServiceInfo
import android.content.res.Configuration
import android.hardware.Sensor
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.os.Build
import android.os.IBinder
import androidx.core.app.ServiceCompat
import androidx.core.content.ContextCompat
import com.kintmin.domain.step.usecase.BackupStepSensorUseCase
import com.kintmin.domain.step.usecase.GetAccelerateStepUseCase
import com.kintmin.domain.step.usecase.GetLastStepSensorUseCase
import com.kintmin.domain.step.usecase.GetStepCountUseCase
import com.kintmin.domain.step.usecase.ResetDataOncePerDayUseCase
import com.kintmin.domain.step.usecase.UpdateAccelerateStepUseCase
import com.kintmin.domain.step.usecase.UpdateLastStepSensorUseCase
import com.kintmin.platform.push_notification.PushNotificationManager
import com.kintmin.platform.push_notification.notifications.SensorStepNotification
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
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.flow.sample
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.temporal.ChronoUnit
import javax.inject.Inject

@OptIn(FlowPreview::class)
@AndroidEntryPoint
class StepForegroundService : Service() {

    companion object {

        fun startService(context: Context): Result<Unit> {
            return runCatching {
                val hasPostNotification = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                    ContextCompat.checkSelfPermission(context, permission.POST_NOTIFICATIONS) == PERMISSION_GRANTED
                } else {
                    true
                }

                val hasActivityRecognition = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    ContextCompat.checkSelfPermission(context, permission.ACTIVITY_RECOGNITION) == PERMISSION_GRANTED
                } else {
                    true
                }

                if (hasPostNotification && hasActivityRecognition) {
                    ContextCompat.startForegroundService(context, Intent(context, StepForegroundService::class.java))
                } else {
                    throw SecurityException("권한이 부족합니다. POST_NOTIFICATIONS: $hasPostNotification ACTIVITY_RECOGNITION: $hasActivityRecognition")
                }
            }
        }
    }

    @Inject
    lateinit var pushNotificationManager: PushNotificationManager

    @Inject
    lateinit var resetDataOncePerDayUseCase: ResetDataOncePerDayUseCase

    @Inject
    lateinit var backupStepSensorUseCase: BackupStepSensorUseCase


    @Inject
    lateinit var getStepCountUseCase: GetStepCountUseCase

    @Inject
    lateinit var getLastStepSensorUseCase: GetLastStepSensorUseCase

    @Inject
    lateinit var getAccelerateStepUseCase: GetAccelerateStepUseCase

    @Inject
    lateinit var updateLastStepSensorUseCase: UpdateLastStepSensorUseCase

    @Inject
    lateinit var updateAccelerateStepUseCase: UpdateAccelerateStepUseCase

    private val foregroundServiceScope = CoroutineScope(Dispatchers.Main.immediate + SupervisorJob())

    private var stepSensorListener: SensorEventListener? = null
    private var accelerometerSensorListener: SensorEventListener? = null

    private val currentStep = MutableStateFlow(0)
    private val currentStepSensor = MutableStateFlow<Long?>(null)
    private val currentAccelerateStep = MutableStateFlow(0)

    private val lastCheckedMillis = MutableStateFlow(System.currentTimeMillis())
    private val zoneIdFlow = MutableStateFlow(ZoneId.systemDefault())

    private val dateChangeBroadcastReceiver = DateChangeBroadcastReceiver().apply {
        setOnDateChangedListener(object : DateChangeBroadcastReceiver.OnDateChangedListener {
            override fun onDateChanged() {
                // 날짜가 변경됐다고 감지되는 경우
                checkDailyReset()
            }

            override fun onTimeChanged() {
                // 사용자가 시간을 변경하거나, NTP 보정으로 인해 불리는 경우
                backupPrevStepSensorIfUnitTimeChanged()
            }

            override fun onTimezoneChanged(timeZone: String) {
                // 지역 설정이 변경되는 경우
                // TODO: DB에 zone id 저장해서 지역 변경도 할 수 있음
                val zoneId = runCatching { ZoneId.of(timeZone) }.getOrDefault(ZoneId.systemDefault())
                zoneIdFlow.update { zoneId }
                backupPrevStepSensorIfUnitTimeChanged()
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
        runCatching {
            val notificationData = SensorStepNotification(0, 0)
            ServiceCompat.startForeground(
                this,
                notificationData.id,
                notificationData.createNotification(this),
                @SuppressLint("InlinedApi") ServiceInfo.FOREGROUND_SERVICE_TYPE_HEALTH,
            )
        }.onFailure {
            stopSelf()
            return START_NOT_STICKY
        }

        return START_STICKY
    }

    override fun onCreate() {
        super.onCreate()

        foregroundServiceScope.launch {
            val today = LocalDate.now(zoneIdFlow.value).format(DateTimeFormatter.BASIC_ISO_DATE)
            val todayStepCount = getStepCountUseCase(today)
            currentStep.update { todayStepCount }

            val lastStepSensor = getLastStepSensorUseCase().getOrNull()
            currentStepSensor.update { lastStepSensor }

            val todayAccelerateStepCount = getAccelerateStepUseCase().getOrDefault(0)
            currentAccelerateStep.update { todayAccelerateStepCount }

            registerSensor().onSuccess {
                registerStepResetReceiver()
                registerLightDarkModeSwitchListener()
                observeNotificationData()
                registerStepBackup()
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
        )
        sensorManager.registerListener(stepSensorListener, stepSensor, SensorManager.SENSOR_DELAY_UI)
    }

    private fun registerAccelerometerSensor(): Result<Unit> = runCatching {
        val sensorManager = getSystemService(Context.SENSOR_SERVICE) as SensorManager
        val accelerometerSensor = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)!!

        accelerometerSensorListener = AccelerometerSensorListener(
            updateStep = { updateAccelerateStep() },
        )
        sensorManager.registerListener(
            accelerometerSensorListener,
            accelerometerSensor,
            SensorManager.SENSOR_DELAY_GAME
        )
    }

    private fun registerSensor(): Result<Unit> {
        return registerStepSensor().mapCatching {
            registerAccelerometerSensor().getOrThrow()
        }
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

    private fun unregisterStepResetReceiver() {
        runCatching { unregisterReceiver(dateChangeBroadcastReceiver) }
    }

    private fun registerLightDarkModeSwitchListener() {
        registerComponentCallbacks(lightDarkModeSwitchListener)
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
                .onStart {
                    updateForegroundNotification()
                }
                .sample(1000L)
                .collectLatest {
                    updateForegroundNotification()
                }
        }
    }

    private fun registerStepBackup() {
        // datastore는 즉시 update
        foregroundServiceScope.launch {
            currentStepSensor.filterNotNull().collect {
                updateLastStepSensorUseCase(it)
            }
        }
        foregroundServiceScope.launch {
            currentAccelerateStep.collectLatest {
                updateAccelerateStepUseCase(it)
            }
        }
    }

    private fun updateForegroundNotification() {
        pushNotificationManager.sendNotification(
            SensorStepNotification(
                currentStep.value,
                currentAccelerateStep.value,
            )
        )
    }

    private fun updateStep(newStepSensor: Long) {
        val prevStepSensor = currentStepSensor.value

        if (prevStepSensor != null) {
            if (newStepSensor > prevStepSensor) {
                val stepDelta = (newStepSensor - prevStepSensor).toInt()
                currentStep.update { prevStep -> prevStep + stepDelta }
                backupPrevStepSensorIfUnitTimeChanged()
            } else {    // 센서가 작아진 경우 = 재부팅 or 기기 이슈
                foregroundServiceScope.launch {
                    val currentMillis = System.currentTimeMillis()
                    backupStepSensorUseCase(newStepSensor, currentMillis)
                }
            }
        }

        currentStepSensor.update { newStepSensor }
    }

    private fun backupPrevStepSensorIfUnitTimeChanged() {
        val baseSensorStep = currentStepSensor.value ?: return

        val zoneId = zoneIdFlow.value
        val prevCheckedMillis = lastCheckedMillis.value
        val currentMillis = System.currentTimeMillis()

        val prevHalfHourMillis = toHalfHourMillis(prevCheckedMillis, zoneId)
        val currentHalfHourMillis = toHalfHourMillis(currentMillis, zoneId)

        if (prevHalfHourMillis != currentHalfHourMillis) {
            val currentDateTime = Instant.ofEpochMilli(currentHalfHourMillis).atZone(zoneIdFlow.value)
            if (currentDateTime.hour == 0) {
                checkDailyReset()
            } else {
                foregroundServiceScope.launch {
                    backupStepSensorUseCase(baseSensorStep, currentHalfHourMillis)
                }
            }
        }

        lastCheckedMillis.update { currentMillis }
    }

    fun toHalfHourMillis(millis: Long, zoneId: ZoneId): Long {
        val zoned = Instant.ofEpochMilli(millis).atZone(zoneId)
        val truncatedMinute = if (zoned.minute < 30) 0 else 30

        return zoned
            .withMinute(truncatedMinute)
            .withSecond(0)
            .withNano(0)
            .toInstant()
            .toEpochMilli()
    }

    private fun checkDailyReset() {
        resetDataOncePerDayUseCase.invoke(currentStep.value, currentStepSensor.value, zoneIdFlow.value) {
            currentStep.update { 0 }
            currentAccelerateStep.update { 0 }
        }
    }

    private fun updateAccelerateStep() {
        currentAccelerateStep.update { it + 1 }
    }
}