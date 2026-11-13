package com.kintmin.platform.service

import android.annotation.SuppressLint
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.net.wifi.WifiManager
import android.os.Build
import android.os.IBinder
import android.os.PowerManager
import androidx.core.app.NotificationManagerCompat
import androidx.core.app.ServiceCompat
import com.kintmin.domain.data_migration.model.DataMigrationSendStatus
import com.kintmin.domain.data_migration.usecase.CloseMigrationSendUseCase
import com.kintmin.domain.data_migration.usecase.FetchMigrationSendStatusUseCase
import com.kintmin.domain.file_share.usecase.StopPcFileShareServerUseCase
import com.kintmin.platform.push_notification.PushNotificationIds
import com.kintmin.platform.push_notification.notifications.DataMigrationSendNotification
import com.kintmin.platform.push_notification.notifications.FileShareServerNotification
import com.kintmin.platform.push_notification.notifications.PushNotification
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancelChildren
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject

class FileShareForegroundService : Service(), KoinComponent {

    private val fetchMigrationSendStatusUseCase: FetchMigrationSendStatusUseCase by inject()
    private val stopPcFileShareServerUseCase: StopPcFileShareServerUseCase by inject()
    private val closeMigrationSendUseCase: CloseMigrationSendUseCase by inject()

    private val serviceScope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    private var isForeground = false
    private var migrationWakeLock: PowerManager.WakeLock? = null
    private var migrationWifiLock: WifiManager.WifiLock? = null
    private var migrationLockJob: Job? = null
    private var migrationWakeLockRenewJob: Job? = null
    private var generation = NO_GENERATION

    enum class Owner { PC_FILE_SHARE, DATA_MIGRATION }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (intent?.action == ACTION_STOP) {
            if (Owner.DATA_MIGRATION !in owners.value) stopSelf()
            return START_NOT_STICKY
        }
        if (intent?.action == ACTION_REPOST_NOTIFICATION) {
            if (isForeground && Owner.DATA_MIGRATION in owners.value) startForegroundWith(DataMigrationSendNotification)
            return START_NOT_STICKY
        }
        intent?.getIntExtra(EXTRA_GENERATION, NO_GENERATION)?.takeIf { it != NO_GENERATION }?.let { generation = it }
        if (isForeground) return START_NOT_STICKY

        val notification = if (Owner.DATA_MIGRATION in owners.value) DataMigrationSendNotification else FileShareServerNotification
        runCatching { startForegroundWith(notification) }.onSuccess {
            isForeground = true
            holdLocksWhileMigrating()
        }.onFailure {
            stopSelf()
            return START_NOT_STICKY
        }

        return START_NOT_STICKY
    }

    private fun startForegroundWith(notification: PushNotification) {
        ServiceCompat.startForeground(
            this,
            notification.id,
            notification.createNotification(this),
            @SuppressLint("InlinedApi") ServiceInfo.FOREGROUND_SERVICE_TYPE_DATA_SYNC,
        )
    }

    override fun onDestroy() {
        // 알림의 중지나 OS 가 서비스를 내린 경우다. 서버도 함께 내린다.
        if (clearOwnersIfCurrent(generation)) {
            stopPcFileShareServerUseCase()
            closeMigrationSendUseCase()
        }
        releaseMigrationLocks()
        serviceScope.coroutineContext.cancelChildren()
        super.onDestroy()
    }

    /**
     * 포그라운드 서비스는 프로세스를 살려 둘 뿐 CPU 와 Wi-Fi 를 깨워 두지 않는다. 화면이 꺼진 채 받는
     * 기기가 파일을 받아 가는 동안 멈추지 않도록 둘을 잡는다. 완료 뒤에도 실패한 곡을 다시 받을 수 있어
     * 세션이 닫힐 때까지 놓지 않는다.
     */
    private fun holdLocksWhileMigrating() {
        if (migrationLockJob?.isActive == true) return
        migrationLockJob = serviceScope.launch {
            fetchMigrationSendStatusUseCase().collect { status ->
                val isConnected = status is DataMigrationSendStatus.Paired || status is DataMigrationSendStatus.Finished
                if (isConnected) acquireMigrationLocks() else releaseMigrationLocks()
            }
        }
    }

    @Synchronized
    private fun acquireMigrationLocks() {
        if (migrationWakeLock?.isHeld != true) {
            migrationWakeLock = (getSystemService(POWER_SERVICE) as PowerManager)
                .newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, MIGRATION_LOCK_TAG)
                // 다시 잡아도 개수가 쌓이지 않아야 release 한 번으로 놓인다.
                .apply { setReferenceCounted(false) }
                .apply { acquire(MIGRATION_LOCK_TIMEOUT_MS) }
            renewWakeLockBeforeTimeout()
        }
        if (migrationWifiLock?.isHeld != true) {
            val mode = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                WifiManager.WIFI_MODE_FULL_LOW_LATENCY
            } else {
                @Suppress("DEPRECATION")
                WifiManager.WIFI_MODE_FULL_HIGH_PERF
            }
            migrationWifiLock = (applicationContext.getSystemService(WIFI_SERVICE) as WifiManager)
                .createWifiLock(mode, MIGRATION_LOCK_TAG)
                .apply { acquire() }
        }
    }

    private fun renewWakeLockBeforeTimeout() {
        migrationWakeLockRenewJob?.cancel()
        migrationWakeLockRenewJob = serviceScope.launch {
            while (true) {
                delay(MIGRATION_LOCK_RENEW_MS)
                migrationWakeLock?.acquire(MIGRATION_LOCK_TIMEOUT_MS)
            }
        }
    }

    @Synchronized
    private fun releaseMigrationLocks() {
        migrationWakeLockRenewJob?.cancel()
        migrationWakeLockRenewJob = null
        migrationWakeLock?.takeIf { it.isHeld }?.release()
        migrationWakeLock = null
        migrationWifiLock?.takeIf { it.isHeld }?.release()
        migrationWifiLock = null
    }

    companion object {
        const val ACTION_STOP = "com.kintmin.platform.FILE_SHARE_STOP"
        const val ACTION_REPOST_NOTIFICATION = "com.kintmin.platform.FILE_SHARE_REPOST_NOTIFICATION"
        private const val EXTRA_GENERATION = "generation"
        private const val NO_GENERATION = -1

        private const val MIGRATION_LOCK_TAG = "JellyTube:migration"

        // 놓친 해제가 배터리를 끝까지 잡지 않도록 상한을 둔다.
        private const val MIGRATION_LOCK_TIMEOUT_MS = 2 * 60 * 60 * 1000L
        private const val MIGRATION_LOCK_RENEW_MS = MIGRATION_LOCK_TIMEOUT_MS - 10 * 60 * 1000L

        private val _owners = MutableStateFlow<Set<Owner>>(emptySet())
        val owners = _owners.asStateFlow()

        private var startedGeneration = NO_GENERATION

        @Synchronized
        private fun setOwners(owners: Set<Owner>) {
            _owners.value = owners
        }

        /** 나갔다 바로 다시 들어오면 앞 서비스의 onDestroy 가 새로 연 뒤에 온다. 마지막으로 띄운 서비스일 때만 비운다. */
        @Synchronized
        private fun clearOwnersIfCurrent(generation: Int): Boolean {
            if (generation != startedGeneration) return false
            setOwners(emptySet())
            return true
        }

        @Synchronized
        fun startService(context: Context, owner: Owner): Result<Unit> = runCatching {
            val previous = _owners.value
            setOwners(previous + owner)
            if (previous.isEmpty()) {
                try {
                    NotificationManagerCompat.from(context).cancel(PushNotificationIds.FILE_SHARE_SERVER)
                    startedGeneration++
                    context.startForegroundService(
                        Intent(context, FileShareForegroundService::class.java)
                            .putExtra(EXTRA_GENERATION, startedGeneration),
                    )
                } catch (e: Throwable) {
                    setOwners(previous)
                    throw e
                }
            }
        }

        @Synchronized
        fun stopService(context: Context, owner: Owner): Result<Unit> = runCatching {
            setOwners(_owners.value - owner)
            if (_owners.value.isEmpty()) {
                context.stopService(Intent(context, FileShareForegroundService::class.java))
            }
        }
    }
}
