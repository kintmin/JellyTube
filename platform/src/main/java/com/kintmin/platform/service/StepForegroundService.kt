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
import com.kintmin.domain.step.usecase.GetLastStepSensorUseCase
import com.kintmin.domain.step.usecase.GetStepCountUseCase
import com.kintmin.domain.step.usecase.ResetDataOncePerDayUseCase
import com.kintmin.domain.step.usecase.UpdateLastStepSensorUseCase
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
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.flow.sample
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter
import javax.inject.Inject

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

                val hasStepCounterSensor = (context.getSystemService(Context.SENSOR_SERVICE) as? SensorManager)?.getDefaultSensor(Sensor.TYPE_STEP_COUNTER) != null
                if (!hasStepCounterSensor) {
                    throw Exception("걸음수 센서가 없습니다.")
                }

                ContextCompat.startForegroundService(context, Intent(context, StepForegroundService::class.java))
            }
        }
    }

    @Inject lateinit var pushNotificationManager: PushNotificationManager
    @Inject lateinit var resetDataOncePerDayUseCase: ResetDataOncePerDayUseCase
    @Inject lateinit var backupStepSensorUseCase: BackupStepSensorUseCase

    @Inject lateinit var getStepCountUseCase: GetStepCountUseCase
    @Inject lateinit var getLastStepSensorUseCase: GetLastStepSensorUseCase
    @Inject lateinit var updateLastStepSensorUseCase: UpdateLastStepSensorUseCase

    private val foregroundServiceScope = CoroutineScope(Dispatchers.Main.immediate + SupervisorJob())

    private var stepSensorListener: SensorEventListener? = null

    private val currentStep = MutableStateFlow(0)
    private val currentStepSensor = MutableStateFlow<Long?>(null)

    private val zoneIdFlow = MutableStateFlow(ZoneId.systemDefault())

    private val lastBackupUnitMillis = MutableStateFlow<Long?>(null)

    private val dateChangeBroadcastReceiver = DateChangeBroadcastReceiver().apply {
        setOnDateChangedListener(object : DateChangeBroadcastReceiver.OnDateChangedListener {
            override fun onDateChanged() {
                // 날짜가 변경됐다고 감지되는 경우
                checkDailyReset()
            }

            override fun onTimeChanged() {
                // 사용자가 시간을 변경하거나, NTP 보정으로 인해 불리는 경우
                currentStepSensor.value?.let {
                    val currentMillis = System.currentTimeMillis()
                    backupStepSensor(
                        stepSensor = it,
                        currentMillis = currentMillis,
                        saveMillis = currentMillis,
                    )
                }
            }

            override fun onTimezoneChanged(timeZone: String) {
                // 지역 설정이 변경되는 경우
                val zoneId = runCatching { ZoneId.of(timeZone) }.getOrDefault(ZoneId.systemDefault())
                zoneIdFlow.update { zoneId }
                currentStepSensor.value?.let {
                    val currentMillis = System.currentTimeMillis()
                    backupStepSensor(
                        stepSensor = it,
                        currentMillis = currentMillis,
                        saveMillis = currentMillis,
                    )
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
            val notificationData = SensorStepNotification(0)
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
        val lastBackupUnitMillis = lastBackupUnitMillis.value
        val currentMillis = System.currentTimeMillis()

        val isFirstCheck = prevStepSensor == null || lastBackupUnitMillis == null
        val isRebooted = prevStepSensor?.let { newStepSensor < it } ?: false

        if (isFirstCheck || isRebooted) {
            backupStepSensor(
                stepSensor = newStepSensor,
                currentMillis = currentMillis,
                saveMillis = currentMillis,
            )
        } else {
            val stepDelta = (newStepSensor - prevStepSensor).toInt()
            currentStep.update { prevStep -> prevStep + stepDelta }
            backupStepSensor(
                stepSensor = prevStepSensor,
                currentMillis = currentMillis,
                saveMillis = lastBackupUnitMillis + BACKUP_UNIT_MILLIS,
            )
        }

        currentStepSensor.update { newStepSensor }
    }

    private fun backupStepSensor(stepSensor: Long, currentMillis: Long, saveMillis: Long) {
        val currentUnit = truncateToUnitZonedDateTime(currentMillis, zoneIdFlow.value)
        val currentUnitMillis = currentUnit.toInstant().toEpochMilli()

        if (lastBackupUnitMillis.value == currentUnitMillis) return

        if (currentUnit.hour == 0 && currentUnit.minute == 0) {
            checkDailyReset()
        } else {
            foregroundServiceScope.launch {
                backupStepSensorUseCase(stepSensor, saveMillis)
            }
        }

        lastBackupUnitMillis.update { currentUnitMillis }
    }

    fun truncateToUnitZonedDateTime(millis: Long, zoneId: ZoneId): ZonedDateTime {
        val unitMinutes = (BACKUP_UNIT_MILLIS / 60_000L).toInt()
        val zoned = Instant.ofEpochMilli(millis).atZone(zoneId)
        val truncatedMinute = (zoned.minute / unitMinutes) * unitMinutes

        return zoned
            .withMinute(truncatedMinute)
            .withSecond(0)
            .withNano(0)
    }

    private fun checkDailyReset() {
        resetDataOncePerDayUseCase.invoke(currentStep.value, currentStepSensor.value, zoneIdFlow.value) {
            currentStep.update { 0 }
        }
    }
}