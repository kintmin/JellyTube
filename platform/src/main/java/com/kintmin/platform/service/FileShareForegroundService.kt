package com.kintmin.platform.service

import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.net.nsd.NsdManager
import android.net.nsd.NsdServiceInfo
import android.os.Build
import android.os.IBinder
import androidx.core.app.ServiceCompat
import com.kintmin.domain.audio_media.usecase.AlreadyDownloadedMedia
import com.kintmin.domain.audio_media.usecase.ImportUploadedAudioMediaUseCase
import com.kintmin.fileshare.FileShareConstants
import com.kintmin.fileshare.UploadResponse
import com.kintmin.platform.push_notification.PushNotificationManager
import com.kintmin.platform.push_notification.notifications.DownloadResultNotification
import com.kintmin.platform.push_notification.notifications.FileShareServerNotification
import dagger.hilt.android.AndroidEntryPoint
import io.ktor.http.HttpStatusCode
import io.ktor.serialization.kotlinx.json.json
import io.ktor.server.application.install
import io.ktor.server.cio.CIO
import io.ktor.server.engine.embeddedServer
import io.ktor.server.plugins.contentnegotiation.ContentNegotiation
import io.ktor.server.request.header
import io.ktor.server.request.receiveChannel
import io.ktor.server.response.respond
import io.ktor.server.routing.post
import io.ktor.server.routing.routing
import io.ktor.server.websocket.WebSockets
import io.ktor.server.websocket.webSocket
import io.ktor.utils.io.jvm.javaio.toInputStream
import io.ktor.websocket.Frame
import io.ktor.websocket.send
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import kotlinx.serialization.json.Json
import javax.inject.Inject

@AndroidEntryPoint
class FileShareForegroundService : Service() {

    @Inject lateinit var importUploadedAudioMediaUseCase: ImportUploadedAudioMediaUseCase
    @Inject lateinit var pushNotificationManager: PushNotificationManager

    private val serviceScope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    private var ktorServer: io.ktor.server.engine.EmbeddedServer<*, *>? = null
    private var nsdManager: NsdManager? = null
    private var registeredServiceName: String? = null
    private var nsdRegistrationListener: NsdManager.RegistrationListener? = null

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (intent?.action == ACTION_STOP) {
            stopSelf()
            return START_NOT_STICKY
        }

        startForeground()
        startKtorServer()
        registerNsdService()

        return START_NOT_STICKY
    }

    override fun onDestroy() {
        unregisterNsdService()
        serviceScope.launch {
            ktorServer?.let { server ->
                runCatching { server.stop(500L, 500L) }
            }
        }
        serviceScope.cancel()
        super.onDestroy()
    }

    private fun startForeground() {
        runCatching {
            val notification = FileShareServerNotification.createNotification(this)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                ServiceCompat.startForeground(
                    this,
                    FileShareServerNotification.id,
                    notification,
                    ServiceInfo.FOREGROUND_SERVICE_TYPE_DATA_SYNC,
                )
            } else {
                startForeground(FileShareServerNotification.id, notification)
            }
        }.onFailure { stopSelf() }
    }

    private fun startKtorServer() {
        serviceScope.launch {
            runCatching {
                val importUseCase = importUploadedAudioMediaUseCase
                val notificationManager = pushNotificationManager

                ktorServer = embeddedServer(CIO, port = FileShareConstants.DEFAULT_PORT) {
                    install(WebSockets)
                    install(ContentNegotiation) {
                        json(Json { ignoreUnknownKeys = true })
                    }
                    routing {
                        webSocket(FileShareConstants.WS_STATUS_PATH) {
                            send("running")
                            for (frame in incoming) {
                                if (frame is Frame.Close) break
                            }
                        }

                        /**
                         * POST /upload
                         * Header: X-File-Name: <original file name with extension>
                         * Body: raw audio bytes
                         */
                        post(FileShareConstants.HTTP_UPLOAD_PATH) {
                            val originalFileName = call.request.header(FileShareConstants.HEADER_FILE_NAME)
                                ?: "upload.mp3"

                            val bytes = runCatching {
                                call.receiveChannel().toInputStream().readBytes()
                            }.getOrElse {
                                call.respond(
                                    HttpStatusCode.BadRequest,
                                    UploadResponse(success = false, message = "파일을 읽을 수 없습니다."),
                                )
                                return@post
                            }

                            importUseCase(bytes, originalFileName)
                                .onSuccess { audioMedia ->
                                    notificationManager.sendNotification(
                                        DownloadResultNotification(
                                            resultType = DownloadResultNotification.ResultType.Success,
                                            contentText = "${audioMedia.artist} - ${audioMedia.name}",
                                            playlistId = null,
                                            audioMediaId = audioMedia.id,
                                        ),
                                    )
                                    call.respond(
                                        HttpStatusCode.OK,
                                        UploadResponse(
                                            success = true,
                                            message = "업로드 성공",
                                            audioMediaId = audioMedia.id,
                                            title = audioMedia.name,
                                        ),
                                    )
                                }
                                .onFailure { error ->
                                    val message = when (error) {
                                        is AlreadyDownloadedMedia -> "이미 저장된 파일입니다."
                                        else -> error.message ?: "업로드 실패"
                                    }
                                    call.respond(
                                        HttpStatusCode.UnprocessableEntity,
                                        UploadResponse(success = false, message = message),
                                    )
                                }
                        }
                    }
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
            nsdManager = getSystemService(Context.NSD_SERVICE) as NsdManager
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

        fun startService(context: Context) {
            val intent = Intent(context, FileShareForegroundService::class.java)
            context.startForegroundService(intent)
        }

        fun stopService(context: Context) {
            context.stopService(Intent(context, FileShareForegroundService::class.java))
        }
    }
}
