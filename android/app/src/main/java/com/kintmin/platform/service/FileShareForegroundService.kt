package com.kintmin.platform.service

import android.annotation.SuppressLint
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.net.nsd.NsdManager
import android.net.nsd.NsdServiceInfo
import android.os.IBinder
import androidx.core.app.NotificationManagerCompat
import androidx.core.app.ServiceCompat
import com.kintmin.domain.audio_media.usecase.ImportUploadedAudioMediaUseCase
import com.kintmin.domain.audio_media.usecase.SaveAudioMediaImageUseCase
import com.kintmin.domain.audio_media.usecase.UpdateAudioMediaUseCase
import com.kintmin.domain.data_migration.usecase.ExportMigrationDataUseCase
import com.kintmin.domain.data_migration.usecase.ResolveMigrationFilePathUseCase
import com.kintmin.fileshare.FileShareConstants
import com.kintmin.platform.push_notification.PushNotificationIds
import com.kintmin.platform.push_notification.PushNotificationManager
import com.kintmin.platform.push_notification.notifications.DownloadResultNotification
import com.kintmin.platform.push_notification.notifications.FileShareServerNotification
import com.kintmin.platform.service.migration.MigrationSessionHolder
import io.ktor.server.cio.CIO
import io.ktor.server.engine.embeddedServer
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancelChildren
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
    private val exportMigrationDataUseCase: ExportMigrationDataUseCase by inject()
    private val resolveMigrationFilePathUseCase: ResolveMigrationFilePathUseCase by inject()
    private val pushNotificationManager: PushNotificationManager by inject()

    private val serviceScope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    private var ktorServer: io.ktor.server.engine.EmbeddedServer<*, *>? = null
    private var nsdManager: NsdManager? = null
    private var registeredServiceName: String? = null
    private var nsdRegistrationListener: NsdManager.RegistrationListener? = null

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (intent?.action == ACTION_STOP) {
            _isRunning.update { false }
            stopSelf()
            return START_NOT_STICKY
        }

        runCatching {
            ServiceCompat.startForeground(
                this,
                FileShareServerNotification.id,
                FileShareServerNotification.createNotification(this),
                @SuppressLint("InlinedApi") ServiceInfo.FOREGROUND_SERVICE_TYPE_DATA_SYNC,
            )
        }.onSuccess {
            _isRunning.update { true }
            startKtorServer()
            registerNsdService()
        }.onFailure {
            _isRunning.update { false }
            stopSelf()
            return START_NOT_STICKY
        }

        return START_NOT_STICKY
    }

    override fun onDestroy() {
        _isRunning.update { false }
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
                                    resultType = DownloadResultNotification.ResultType.Success,
                                    contentText = "${result.audioMedia.artist} - ${result.audioMedia.name}",
                                    playlistId = result.playlistIdOnDownload,
                                    audioMediaId = result.audioMedia.id,
                                ),
                            )
                        },
                        currentMigrationSession = { MigrationSessionHolder.session.value },
                        exportMigrationData = exportMigrationDataUseCase,
                        resolveMigrationFilePath = resolveMigrationFilePathUseCase,
                    )
                }.start(wait = false)
            }
        }
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

        private val _isRunning = MutableStateFlow(false)
        val isRunning = _isRunning.asStateFlow()

        fun startService(context: Context): Result<Unit> {
            return runCatching<Unit> {
                NotificationManagerCompat.from(context).cancel(PushNotificationIds.FILE_SHARE_SERVER)
                context.startForegroundService(Intent(context, FileShareForegroundService::class.java))
            }.onSuccess {
                _isRunning.update { true }
            }
        }

        fun stopService(context: Context): Result<Unit> {
            return runCatching<Unit> {
                context.stopService(Intent(context, FileShareForegroundService::class.java))
            }.onSuccess {
                _isRunning.update { false }
            }
        }
    }
}
