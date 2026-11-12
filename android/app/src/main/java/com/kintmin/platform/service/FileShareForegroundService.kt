package com.kintmin.platform.service

import android.annotation.SuppressLint
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.net.nsd.NsdManager
import android.net.nsd.NsdServiceInfo
import android.net.wifi.WifiManager
import android.os.Build
import android.os.IBinder
import android.os.PowerManager
import androidx.core.app.NotificationManagerCompat
import androidx.core.app.ServiceCompat
import com.kintmin.domain.audio_media.usecase.CleanupAnomalousDataUseCase
import com.kintmin.domain.audio_media.usecase.ImportUploadedAudioMediaUseCase
import com.kintmin.domain.audio_media.usecase.SaveAudioMediaImageUseCase
import com.kintmin.domain.audio_media.usecase.UpdateAudioMediaUseCase
import com.kintmin.domain.data_migration.usecase.ExportMigrationDataUseCase
import com.kintmin.domain.data_migration.usecase.ResolveMigrationFilePathUseCase
import com.kintmin.fileshare.FileShareConstants
import com.kintmin.platform.push_notification.PushNotificationIds
import com.kintmin.platform.push_notification.PushNotificationManager
import com.kintmin.platform.push_notification.notifications.DataMigrationSendNotification
import com.kintmin.platform.push_notification.notifications.DownloadResultNotification
import com.kintmin.platform.push_notification.notifications.FileShareServerNotification
import com.kintmin.platform.push_notification.notifications.PushNotification
import com.kintmin.platform.service.migration.MigrationSessionHolder
import com.kintmin.presentation.port.WorkResultNotice
import com.kintmin.presentation.port.model.DataMigrationSendStatus
import io.ktor.server.cio.CIO
import io.ktor.server.engine.embeddedServer
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancelChildren
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject

class FileShareForegroundService : Service(), KoinComponent {

    private val importUploadedAudioMediaUseCase: ImportUploadedAudioMediaUseCase by inject()
    private val updateAudioMediaUseCase: UpdateAudioMediaUseCase by inject()
    private val saveAudioMediaImageUseCase: SaveAudioMediaImageUseCase by inject()
    private val cleanupAnomalousDataUseCase: CleanupAnomalousDataUseCase by inject()
    private val exportMigrationDataUseCase: ExportMigrationDataUseCase by inject()
    private val resolveMigrationFilePathUseCase: ResolveMigrationFilePathUseCase by inject()
    private val pushNotificationManager: PushNotificationManager by inject()

    private val serviceScope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    private var ktorServer: io.ktor.server.engine.EmbeddedServer<*, *>? = null
    private var nsdManager: NsdManager? = null
    private var registeredServiceName: String? = null
    private var nsdRegistrationListener: NsdManager.RegistrationListener? = null
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
            if (ktorServer != null && Owner.DATA_MIGRATION in owners.value) startForegroundWith(DataMigrationSendNotification)
            return START_NOT_STICKY
        }
        intent?.getIntExtra(EXTRA_GENERATION, NO_GENERATION)?.takeIf { it != NO_GENERATION }?.let { generation = it }
        // 다시 붙이면 같은 포트에 서버가 두 번 뜬다.
        if (ktorServer != null) return START_NOT_STICKY

        val notification = if (Owner.DATA_MIGRATION in owners.value) DataMigrationSendNotification else FileShareServerNotification
        runCatching { startForegroundWith(notification) }.onSuccess {
            startKtorServer()
            registerNsdService()
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
        clearOwnersIfCurrent(generation)
        releaseMigrationLocks()
        unregisterNsdService()
        ktorServer?.let { server ->
            runCatching { server.stop(500L, 500L) }
        }
        ktorServer = null
        serviceScope.coroutineContext.cancelChildren()
        super.onDestroy()
    }

    private fun startKtorServer() {
        serviceScope.launch {
            runCatching {
                val importUseCase = importUploadedAudioMediaUseCase
                val updateUseCase = updateAudioMediaUseCase
                val saveImageUseCase = saveAudioMediaImageUseCase
                val notificationManager = pushNotificationManager
                val uploadCacheDir = cacheDir

                ktorServer = embeddedServer(CIO, port = FileShareConstants.DEFAULT_PORT) {
                    fileShareModule(
                        importUploadedAudioMedia = importUseCase,
                        updateAudioMedia = updateUseCase,
                        saveAudioMediaImage = saveImageUseCase,
                        uploadCacheDir = uploadCacheDir,
                        onUploadImported = { result ->
                            notificationManager.sendNotification(
                                DownloadResultNotification(
                                    WorkResultNotice.Download.Success(
                                        artist = result.audioMedia.artist,
                                        name = result.audioMedia.name,
                                        playlistId = result.playlistIdOnDownload,
                                        audioMediaId = result.audioMedia.id,
                                    ),
                                ),
                            )
                        },
                        currentMigrationSession = { MigrationSessionHolder.session.value },
                        cleanupAnomalousData = cleanupAnomalousDataUseCase,
                        exportMigrationData = exportMigrationDataUseCase,
                        resolveMigrationFilePath = resolveMigrationFilePathUseCase,
                    )
                }.start(wait = false)
            }
        }
    }

    /**
     * 포그라운드 서비스는 프로세스를 살려 둘 뿐 CPU 와 Wi-Fi 를 깨워 두지 않는다. 화면이 꺼진 채 받는
     * 기기가 파일을 받아 가는 동안 멈추지 않도록 둘을 잡는다. 완료 뒤에도 실패한 곡을 다시 받을 수 있어
     * 세션이 닫힐 때까지 놓지 않는다.
     */
    private fun holdLocksWhileMigrating() {
        if (migrationLockJob?.isActive == true) return
        migrationLockJob = serviceScope.launch {
            MigrationSessionHolder.state.collect { status ->
                val isConnected = status == DataMigrationSendStatus.Paired || status is DataMigrationSendStatus.Finished
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

    private fun registerNsdService() {
        runCatching {
            val serviceInfo = NsdServiceInfo().apply {
                serviceType = FileShareConstants.NSD_SERVICE_TYPE
                serviceName = FileShareConstants.NSD_SERVICE_NAME
                port = FileShareConstants.DEFAULT_PORT
            }

            val listener = object : NsdManager.RegistrationListener {
                override fun onRegistrationFailed(info: NsdServiceInfo, errorCode: Int) {}
                override fun onUnregistrationFailed(info: NsdServiceInfo, errorCode: Int) {}
                override fun onServiceRegistered(info: NsdServiceInfo) {
                    registeredServiceName = info.serviceName
                }
                override fun onServiceUnregistered(info: NsdServiceInfo) {}
            }

            nsdRegistrationListener = listener
            nsdManager = getSystemService(NSD_SERVICE) as NsdManager
            nsdManager?.registerService(serviceInfo, NsdManager.PROTOCOL_DNS_SD, listener)
        }
    }

    private fun unregisterNsdService() {
        runCatching {
            val listener = nsdRegistrationListener ?: return
            nsdManager?.unregisterService(listener)
            nsdRegistrationListener = null
            nsdManager = null
        }
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

        private val _isPcFileShareRunning = MutableStateFlow(false)
        val isPcFileShareRunning = _isPcFileShareRunning.asStateFlow()

        private var startedGeneration = NO_GENERATION

        @Synchronized
        private fun setOwners(owners: Set<Owner>) {
            _owners.value = owners
            _isPcFileShareRunning.value = Owner.PC_FILE_SHARE in owners
        }

        /** 나갔다 바로 다시 들어오면 앞 서비스의 onDestroy 가 새로 연 뒤에 온다. 마지막으로 띄운 서비스일 때만 비운다. */
        @Synchronized
        private fun clearOwnersIfCurrent(generation: Int) {
            if (generation != startedGeneration) return
            setOwners(emptySet())
            MigrationSessionHolder.close()
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
