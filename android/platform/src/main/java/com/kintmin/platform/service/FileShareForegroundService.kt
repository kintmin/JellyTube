package com.kintmin.platform.service

import android.annotation.SuppressLint
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.net.nsd.NsdManager
import android.net.nsd.NsdServiceInfo
import android.os.IBinder
import android.os.SystemClock
import androidx.core.app.NotificationManagerCompat
import androidx.core.app.ServiceCompat
import com.kintmin.domain.audio_media.usecase.AlreadyDownloadedMedia
import com.kintmin.domain.audio_media.usecase.CleanupUploadedAudioStagingFilesUseCase
import com.kintmin.domain.audio_media.usecase.CreateUploadedAudioStagingFileUseCase
import com.kintmin.domain.audio_media.usecase.ImportUploadedAudioMediaUseCase
import com.kintmin.domain.audio_media.usecase.SaveAudioMediaImageUseCase
import com.kintmin.domain.audio_media.usecase.UpdateAudioMediaUseCase
import com.kintmin.domain.audio_media.usecase.ValidateUploadedAudioFileNameUseCase
import com.kintmin.domain.extension.suspendRunCatching
import com.kintmin.domain.hash.Sha256
import com.kintmin.fileshare.BulkArtistUpdateRequest
import com.kintmin.fileshare.FileShareConstants
import com.kintmin.fileshare.FileShareResponse
import com.kintmin.fileshare.UploadResponse
import com.kintmin.log.AppLog
import com.kintmin.log.model.DebugLog
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
import io.ktor.server.request.contentLength
import io.ktor.server.request.header
import io.ktor.server.request.receive
import io.ktor.server.request.receiveChannel
import io.ktor.server.response.respond
import io.ktor.server.routing.post
import io.ktor.server.routing.routing
import io.ktor.server.websocket.WebSockets
import io.ktor.server.websocket.webSocket
import io.ktor.utils.io.jvm.javaio.toInputStream
import io.ktor.utils.io.readAvailable
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
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import java.io.File
import java.io.FileOutputStream

class FileShareForegroundService : Service(), KoinComponent {

    private val createUploadedAudioStagingFileUseCase: CreateUploadedAudioStagingFileUseCase by inject()
    private val cleanupUploadedAudioStagingFilesUseCase: CleanupUploadedAudioStagingFilesUseCase by inject()
    private val validateUploadedAudioFileNameUseCase: ValidateUploadedAudioFileNameUseCase by inject()
    private val importUploadedAudioMediaUseCase: ImportUploadedAudioMediaUseCase by inject()
    private val updateAudioMediaUseCase: UpdateAudioMediaUseCase by inject()
    private val saveAudioMediaImageUseCase: SaveAudioMediaImageUseCase by inject()
    private val pushNotificationManager: PushNotificationManager by inject()
    private val appLog: AppLog by inject()

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
            cleanupExpiredStagingFiles()
            startKtorServer()
            registerNsdService()
        }.onFailure { error ->
            log("포그라운드 시작 실패: ${error.describe()}")
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
                .onFailure { error -> log("Ktor 서버 종료 실패: ${error.describe()}") }
        }
        ktorServer = null
        serviceScope.coroutineContext.cancelChildren()
        super.onDestroy()
    }

    /**
     * 수신 도중 프로세스가 죽어 남은 스테이징 파일을 회수한다.
     * 다음 업로드가 영원히 없을 수도 있으므로 서버가 켜지는 시점에 정리한다.
     */
    private fun cleanupExpiredStagingFiles() {
        serviceScope.launch {
            cleanupUploadedAudioStagingFilesUseCase()
                .onSuccess { deletedCount ->
                    if (deletedCount > 0) log("만료된 스테이징 파일 정리: count=$deletedCount")
                }
                .onFailure { error -> log("스테이징 파일 정리 실패: ${error.describe()}") }
        }
    }

    private fun startKtorServer() {
        serviceScope.launch {
            suspendRunCatching {
                val validateFileNameUseCase = validateUploadedAudioFileNameUseCase
                val createStagingUseCase = createUploadedAudioStagingFileUseCase
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
                            // 헤더는 클라이언트가 임의로 채우는 값이다. 수신을 시작하기 전에 걸러
                            // 대용량을 다 받고 나서 저장 단계에서 실패하는 일을 막는다.
                            val originalFileName = validateFileNameUseCase(
                                call.request.header(FileShareConstants.HEADER_FILE_NAME),
                            ).getOrElse { error ->
                                log("업로드 거절: ${error.describe()}")
                                call.respond(
                                    HttpStatusCode.BadRequest,
                                    UploadResponse(success = false, message = error.message ?: "파일 이름이 올바르지 않습니다."),
                                )
                                return@post
                            }
                            val declaredLength = call.request.contentLength()
                            log("업로드 수신 시작: name=$originalFileName, contentLength=${declaredLength ?: "unknown"}")

                            // 최종 보관 위치와 같은 볼륨의 스테이징 파일로 곧장 흘려 쓴다.
                            // 긴 음원(1시간 이상)도 메모리에 올리지 않고, 이후 저장은 복사 없이 이동으로 끝난다.
                            val stagingFilePath = createStagingUseCase().getOrElse { error ->
                                log("스테이징 파일 생성 실패: name=$originalFileName, ${error.describe()}")
                                call.respond(
                                    HttpStatusCode.InternalServerError,
                                    UploadResponse(success = false, message = "저장 공간을 준비할 수 없습니다."),
                                )
                                return@post
                            }
                            val stagingFile = File(stagingFilePath)

                            try {
                                // 수신하면서 SHA-256을 함께 계산해 파일을 다시 읽지 않는다.
                                val hasher = Sha256()
                                val receiveStartedAt = SystemClock.elapsedRealtime()
                                val receivedBytes = suspendRunCatching {
                                    withContext(Dispatchers.IO) {
                                        // 블로킹 InputStream 어댑터를 거치지 않고 채널에서 직접 읽는다.
                                        val channel = call.receiveChannel()
                                        FileOutputStream(stagingFile).use { output ->
                                            val buffer = ByteArray(RECEIVE_BUFFER_SIZE)
                                            var total = 0L
                                            while (true) {
                                                val read = channel.readAvailable(buffer, 0, buffer.size)
                                                if (read == -1) break
                                                output.write(buffer, 0, read)
                                                hasher.update(buffer, 0, read)
                                                total += read
                                            }
                                            // readAvailable은 연결이 끊겨도 -1만 돌려준다. 끊긴 원인을 직접 확인해야
                                            // 중간에 잘린 전송을 정상 종료로 오인하지 않는다.
                                            channel.closedCause?.let { throw it }
                                            total
                                        }
                                    }
                                }.getOrElse { error ->
                                    val failedAfterMs = SystemClock.elapsedRealtime() - receiveStartedAt
                                    log("업로드 수신 실패: name=$originalFileName, elapsedMs=$failedAfterMs, ${error.describe()}")
                                    call.respond(
                                        HttpStatusCode.BadRequest,
                                        UploadResponse(success = false, message = "파일을 읽을 수 없습니다."),
                                    )
                                    return@post
                                }

                                // Content-Length가 있으면 수신량과 대조해 잘린 파일이 등록되는 것을 막는다.
                                if (declaredLength != null && declaredLength != receivedBytes) {
                                    log("업로드 수신 불완전: name=$originalFileName, expected=$declaredLength, received=$receivedBytes")
                                    call.respond(
                                        HttpStatusCode.BadRequest,
                                        UploadResponse(success = false, message = "파일이 온전히 전송되지 않았습니다."),
                                    )
                                    return@post
                                }

                                // 빈 본문은 Content-Length 대조를 통과한다. 메타데이터도 없는 0바이트 항목이
                                // DB에 등록되는 것을 막으려면 여기서 걸러야 한다.
                                if (receivedBytes == 0L) {
                                    log("업로드 수신 거절: name=$originalFileName, 빈 파일")
                                    call.respond(
                                        HttpStatusCode.BadRequest,
                                        UploadResponse(success = false, message = "빈 파일은 저장할 수 없습니다."),
                                    )
                                    return@post
                                }

                                val receiveElapsedMs = SystemClock.elapsedRealtime() - receiveStartedAt
                                val sha256Hex = hasher.digestHex()
                                log(
                                    "업로드 수신 완료: name=$originalFileName, receivedBytes=$receivedBytes, " +
                                        "elapsedMs=$receiveElapsedMs, ${throughputText(receivedBytes, receiveElapsedMs)}, sha256=$sha256Hex",
                                )

                                val importStartedAt = SystemClock.elapsedRealtime()
                                importUseCase(stagingFilePath, sha256Hex, originalFileName)
                                    .onSuccess { result ->
                                        val importElapsedMs = SystemClock.elapsedRealtime() - importStartedAt
                                        log(
                                            "업로드 저장 성공: name=$originalFileName, audioMediaId=${result.audioMedia.id}, " +
                                                "elapsedMs=$importElapsedMs",
                                        )
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
                                        val importElapsedMs = SystemClock.elapsedRealtime() - importStartedAt
                                        log(
                                            "업로드 저장 실패: name=$originalFileName, receivedBytes=$receivedBytes, " +
                                                "elapsedMs=$importElapsedMs, ${error.describe()}",
                                        )
                                        call.respond(
                                            HttpStatusCode.UnprocessableEntity,
                                            UploadResponse(success = false, message = message),
                                        )
                                    }
                            } finally {
                                // 저장에 성공했다면 이미 이동된 뒤라 no-op이고, 실패했다면 스테이징 파일을 정리한다.
                                stagingFile.delete()
                            }
                        }

                        post(FileShareConstants.HTTP_BULK_ARTIST_PATH) {
                            val request = suspendRunCatching { call.receive<BulkArtistUpdateRequest>() }
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

                            val result = suspendRunCatching {
                                request.audioMediaIds.forEach { id ->
                                    updateUseCase(id = id, artist = request.artist).getOrThrow()
                                }
                            }.onFailure { error ->
                                log("아티스트 일괄 변경 실패: ids=${request.audioMediaIds}, ${error.describe()}")
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

                            val bytes = suspendRunCatching {
                                call.receiveChannel().toInputStream().readBytes()
                            }.getOrElse { error ->
                                log("썸네일 수신 실패: ids=$ids, ${error.describe()}")
                                call.respond(
                                    HttpStatusCode.BadRequest,
                                    FileShareResponse(success = false, message = "이미지를 읽을 수 없습니다."),
                                )
                                return@post
                            }
                            val result = suspendRunCatching {
                                ids.forEach { id ->
                                    val imageFileFullPath = saveImageUseCase(bytes).getOrThrow()
                                    updateUseCase(id = id, imageFileFullPath = imageFileFullPath).getOrThrow()
                                }
                            }.onFailure { error ->
                                log("썸네일 일괄 변경 실패: ids=$ids, ${error.describe()}")
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
            }.onFailure { error ->
                log("Ktor 서버 시작 실패: port=${FileShareConstants.DEFAULT_PORT}, ${error.describe()}")
            }
        }
    }

    private fun log(message: String) {
        appLog.sendDebugLog(DebugLog(tag = LOG_TAG, message = message))
    }

    private fun Throwable.describe(): String = "${this::class.simpleName}: ${message ?: "메시지 없음"}"

    /** 로케일에 영향받지 않도록 정수 연산으로 MB/s를 만든다. */
    private fun throughputText(bytes: Long, elapsedMs: Long): String {
        if (bytes <= 0L || elapsedMs <= 0L) return "throughput=측정불가"
        val tenthMbPerSec = bytes * 1000 * 10 / elapsedMs / (1024 * 1024)
        return "throughput=${tenthMbPerSec / 10}.${tenthMbPerSec % 10}MB/s"
    }

    private fun registerNsdService() {
        runCatching {
            val serviceInfo = NsdServiceInfo().apply {
                serviceType = FileShareConstants.NSD_SERVICE_TYPE
                serviceName = FileShareConstants.NSD_SERVICE_NAME
                port = FileShareConstants.DEFAULT_PORT
            }

            val listener = object : NsdManager.RegistrationListener {
                override fun onRegistrationFailed(info: NsdServiceInfo, errorCode: Int) {
                    log("NSD 등록 실패: errorCode=$errorCode")
                }
                override fun onUnregistrationFailed(info: NsdServiceInfo, errorCode: Int) {
                    log("NSD 해제 실패: errorCode=$errorCode")
                }
                override fun onServiceRegistered(info: NsdServiceInfo) {
                    registeredServiceName = info.serviceName
                }
                override fun onServiceUnregistered(info: NsdServiceInfo) {}
            }

            nsdRegistrationListener = listener
            nsdManager = getSystemService(NSD_SERVICE) as NsdManager
            nsdManager?.registerService(serviceInfo, NsdManager.PROTOCOL_DNS_SD, listener)
        }.onFailure { error ->
            log("NSD 등록 요청 실패: ${error.describe()}")
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
        private const val LOG_TAG = "FileShare"

        /** 업로드 수신 버퍼. 크게 잡을수록 채널 read와 파일 write 왕복 횟수가 줄어든다. */
        private const val RECEIVE_BUFFER_SIZE = 64 * 1024

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
