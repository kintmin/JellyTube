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
import androidx.core.app.NotificationManagerCompat
import androidx.core.app.ServiceCompat
import androidx.core.content.ContextCompat
import com.kintmin.domain.app_setting.usecase.FetchIsStepEnabledFlowUseCase
import com.kintmin.domain.step.usecase.BackupStepSensorUseCase
import com.kintmin.domain.step.usecase.GetLastStepSensorForTodayUseCase
import com.kintmin.domain.step.usecase.GetStepCountUseCase
import com.kintmin.domain.step.usecase.ResetDataOncePerDayUseCase
import com.kintmin.domain.step.usecase.UpdateLastStepSensorUseCase
import com.kintmin.platform.push_notification.PushNotificationIds
import com.kintmin.platform.push_notification.PushNotificationManager
import com.kintmin.platform.push_notification.notifications.SensorStepNotification
import com.kintmin.platform.receiver.DateChangeBroadcastReceiver
import com.kintmin.platform.service.sensor.StepCounterSensorListener
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancelChildren
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.flow.sample
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import javax.inject.Inject

/**
 * Known Issue: 자정에 Foreground가 안 켜져 있었다면, 당일 걸음수는 누락될 수 있음.
 */
@OptIn(FlowPreview::class)
@AndroidEntryPoint
class StepForegroundService : Service() {

    companion object {

        const val BACKUP_UNIT_MILLIS = 30 * 60 * 1000L

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

                if (!hasPostNotification || !hasActivityRecognition) {
                    throw SecurityException("권한이 부족합니다. POST_NOTIFICATIONS: $hasPostNotification ACTIVITY_RECOGNITION: $hasActivityRecognition")
                }

                val hasStepCounterSensor =
                    (context.getSystemService(Context.SENSOR_SERVICE) as? SensorManager)?.getDefaultSensor(Sensor.TYPE_STEP_COUNTER) != null
                if (!hasStepCounterSensor) {
                    throw Exception("걸음수 센서가 없습니다.")
                }

                NotificationManagerCompat.from(context).cancel(PushNotificationIds.SENSOR_STEP)
                ContextCompat.startForegroundService(context, Intent(context, StepForegroundService::class.java))
            }
        }
    }

    @Inject lateinit var pushNotificationManager: PushNotificationManager
    @Inject lateinit var resetDataOncePerDayUseCase: ResetDataOncePerDayUseCase
    @Inject lateinit var backupStepSensorUseCase: BackupStepSensorUseCase
    @Inject lateinit var fetchIsStepEnabledFlowUseCase: FetchIsStepEnabledFlowUseCase

    @Inject lateinit var getStepCountUseCase: GetStepCountUseCase
    @Inject lateinit var getLastStepSensorForTodayUseCase: GetLastStepSensorForTodayUseCase
    @Inject lateinit var updateLastStepSensorUseCase: UpdateLastStepSensorUseCase

    private val foregroundServiceScope = CoroutineScope(Dispatchers.Main.immediate + SupervisorJob())

    private var stepSensorListener: SensorEventListener? = null

    private val currentStep = MutableStateFlow(0)
    private val currentStepSensor = MutableStateFlow<Long?>(null)

    // 서비스 꺼진 후 재시작했을 걸음수 복원을 위해 사용
    private var todayLastSavedStepSensorWhenStarted: Long? = null

    private val zoneIdFlow = MutableStateFlow(ZoneId.systemDefault())

    private val stepSensorProcessor = StepSensorProcessor(
        backupUnitMillis = BACKUP_UNIT_MILLIS,
        getZoneId = { zoneIdFlow.value },
        onBackupSensor = { sensor, saveMillis ->
            foregroundServiceScope.launch { backupStepSensorUseCase(sensor, saveMillis) }
        },
        onDailyReset = { checkDailyReset() },
    )

    private val dateChangeBroadcastReceiver = DateChangeBroadcastReceiver().apply {
        setOnDateChangedListener(object : DateChangeBroadcastReceiver.OnDateChangedListener {
            override fun onDateChanged() {
                // 날짜가 변경됐다고 감지되는 경우
                checkDailyReset()
            }

            override fun onTimeChanged() {
                // 사용자가 시간을 변경하거나, NTP 보정으로 인해 불리는 경우
                currentStepSensor.value?.let {
                    stepSensorProcessor.forceBackupWithCurrentTime(it, System.currentTimeMillis())
                }
            }

            override fun onTimezoneChanged(timeZone: String) {
                // 지역 설정이 변경되는 경우
                val zoneId = runCatching { ZoneId.of(timeZone) }.getOrDefault(ZoneId.systemDefault())
                zoneIdFlow.update { zoneId }
                currentStepSensor.value?.let {
                    stepSensorProcessor.forceBackupWithCurrentTime(it, System.currentTimeMillis())
                }
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
            val notificationData = SensorStepNotification(currentStep.value)
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
            val isStepEnabled = fetchIsStepEnabledFlowUseCase().first()
            if (!isStepEnabled) {
                stopSelf()
                return@launch
            }

            val today = LocalDate.now(zoneIdFlow.value).format(DateTimeFormatter.BASIC_ISO_DATE)
            todayLastSavedStepSensorWhenStarted = getLastStepSensorForTodayUseCase(today)
            currentStep.update { getStepCountUseCase(today) }

            // getStepCountUseCase가 suspend되는 동안 자정이 지날 수 있음.
            // 날짜가 바뀐 경우 어제 걸음수를 오늘 걸음수로 사용하지 않도록 0으로 처리.
            checkDailyReset()

            registerStepSensor().onSuccess {
                registerStepResetReceiver()
                registerLightDarkModeSwitchListener()
                syncPushNotification()
                syncLastStepSensor()
            }.onFailure {
                stopSelf()
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
        val stepSensor = sensorManager.getDefaultSensor(Sensor.TYPE_STEP_COUNTER)

        stepSensorListener = StepCounterSensorListener(
            updateStep = { stepsSinceLastReboot -> updateStep(stepsSinceLastReboot) },
        )
        sensorManager.registerListener(stepSensorListener, stepSensor, SensorManager.SENSOR_DELAY_UI)
    }

    private fun unregisterSensor() {
        runCatching {
            val sensorManager = getSystemService(Context.SENSOR_SERVICE) as SensorManager
            stepSensorListener?.let { sensorManager.unregisterListener(it) }
            stepSensorListener = null
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
        runCatching { registerComponentCallbacks(lightDarkModeSwitchListener) }
    }

    private fun unregisterLightDarkModeSwitchListener() {
        runCatching { unregisterComponentCallbacks(lightDarkModeSwitchListener) }
    }

    private fun syncPushNotification() {
        foregroundServiceScope.launch {
            currentStep.onStart {
                updateForegroundNotification()
            }.sample(1000L).collect {
                updateForegroundNotification()
            }
        }
    }

    private fun syncLastStepSensor() {
        foregroundServiceScope.launch {
            currentStepSensor.filterNotNull().collect {
                updateLastStepSensorUseCase(it)
            }
        }
    }

    private fun updateForegroundNotification() {
        pushNotificationManager.sendNotification(
            SensorStepNotification(
                currentStep.value,
            )
        )
    }

    private fun updateStep(newStepSensor: Long) {
        val prevStepSensor = currentStepSensor.value
        stepSensorProcessor.updateStep(prevStepSensor, newStepSensor, System.currentTimeMillis(), todayLastSavedStepSensorWhenStarted) { delta ->
            currentStep.update { it + delta }
        }
        currentStepSensor.update { newStepSensor }
    }

    private fun checkDailyReset() {
        resetDataOncePerDayUseCase.invoke(currentStep.value, currentStepSensor.value, zoneIdFlow.value) {
            currentStep.update { 0 }
            todayLastSavedStepSensorWhenStarted = null
        }
    }
}
