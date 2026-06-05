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
import com.kintmin.domain.audio_media.usecase.AlreadyDownloadedMedia
import com.kintmin.domain.audio_media.usecase.ImportUploadedAudioMediaUseCase
import com.kintmin.domain.audio_media.usecase.SaveAudioMediaImageUseCase
import com.kintmin.domain.audio_media.usecase.UpdateAudioMediaUseCase
import com.kintmin.fileshare.BulkArtistUpdateRequest
import com.kintmin.fileshare.FileShareConstants
import com.kintmin.fileshare.FileShareResponse
import com.kintmin.fileshare.UploadResponse
import com.kintmin.platform.push_notification.PushNotificationIds
import com.kintmin.platform.push_notification.PushNotificationManager
import com.kintmin.platform.push_notification.notifications.DownloadResultNotification
import com.kintmin.platform.push_notification.notifications.FileShareServerNotification
import io.ktor.http.HttpStatusCode
import io.ktor.serialization.kotlinx.json.json
import io.ktor.server.application.install
import io.ktor.server.cio.CIO
import io.ktor.server.engine.embeddedServer
import io.ktor.server.plugins.contentnegotiation.ContentNegotiation
import io.ktor.server.request.header
import io.ktor.server.request.receive
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
import kotlinx.coroutines.cancelChildren
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.serialization.json.Json
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject

class FileShareForegroundService : Service(), KoinComponent {

    private val importUploadedAudioMediaUseCase: ImportUploadedAudioMediaUseCase by inject()
    private val updateAudioMediaUseCase: UpdateAudioMediaUseCase by inject()
    private val saveAudioMediaImageUseCase: SaveAudioMediaImageUseCase by inject()
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
                                .onSuccess { result ->
                                    notificationManager.sendNotification(
                                        DownloadResultNotification(
                                            resultType = DownloadResultNotification.ResultType.Success,
                                            contentText = "${result.audioMedia.artist} - ${result.audioMedia.name}",
                                            playlistId = result.playlistIdOnDownload,
                                            audioMediaId = result.audioMedia.id,
                                        ),
                                    )
                                    call.respond(
                                        HttpStatusCode.OK,
                                        UploadResponse(
                                            success = true,
                                            message = "업로드 성공",
                                            audioMediaId = result.audioMedia.id,
                                            title = result.audioMedia.name,
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

                        post(FileShareConstants.HTTP_BULK_ARTIST_PATH) {
                            val request = runCatching { call.receive<BulkArtistUpdateRequest>() }
                                .getOrElse {
                                    call.respond(
                                        HttpStatusCode.BadRequest,
                                        FileShareResponse(success = false, message = "요청을 읽을 수 없습니다."),
                                    )
                                    return@post
                                }
                            if (request.audioMediaIds.isEmpty() || request.artist.isBlank()) {
                                call.respond(
                                    HttpStatusCode.BadRequest,
                                    FileShareResponse(success = false, message = "적용할 음원이나 아티스트가 없습니다."),
                                )
                                return@post
                            }

                            val result = runCatching {
                                request.audioMediaIds.forEach { id ->
                                    updateUseCase(id = id, artist = request.artist).getOrThrow()
                                }
                            }
                            call.respond(
                                if (result.isSuccess) HttpStatusCode.OK else HttpStatusCode.UnprocessableEntity,
                                FileShareResponse(
                                    success = result.isSuccess,
                                    message = result.exceptionOrNull()?.message ?: "아티스트 적용 완료",
                                ),
                            )
                        }

                        post(FileShareConstants.HTTP_BULK_THUMBNAIL_PATH) {
                            val ids = call.request.header(FileShareConstants.HEADER_AUDIO_MEDIA_IDS)
                                ?.split(",")
                                ?.mapNotNull { it.trim().toIntOrNull() }
                                .orEmpty()
                            if (ids.isEmpty()) {
                                call.respond(
                                    HttpStatusCode.BadRequest,
                                    FileShareResponse(success = false, message = "적용할 음원이 없습니다."),
                                )
                                return@post
                            }

                            val bytes = runCatching {
                                call.receiveChannel().toInputStream().readBytes()
                            }.getOrElse {
                                call.respond(
                                    HttpStatusCode.BadRequest,
                                    FileShareResponse(success = false, message = "이미지를 읽을 수 없습니다."),
                                )
                                return@post
                            }
                            val result = runCatching {
                                ids.forEach { id ->
                                    val imageFileFullPath = saveImageUseCase(bytes).getOrThrow()
                                    updateUseCase(id = id, imageFileFullPath = imageFileFullPath).getOrThrow()
                                }
                            }
                            call.respond(
                                if (result.isSuccess) HttpStatusCode.OK else HttpStatusCode.UnprocessableEntity,
                                FileShareResponse(
                                    success = result.isSuccess,
                                    message = result.exceptionOrNull()?.message ?: "썸네일 적용 완료",
                                ),
                            )
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
