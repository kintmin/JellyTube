package com.kintmin.presentation.port

import com.kintmin.domain.audio_media.usecase.ImportUploadedAudioMediaUseCase
import com.kintmin.domain.audio_media.usecase.SaveAudioMediaImageUseCase
import com.kintmin.domain.audio_media.usecase.UpdateAudioMediaUseCase
import com.kintmin.domain.audio_media.usecase.ValidateUploadedAudioFileNameUseCase
import com.kintmin.fileshare.FileShareConstants
import io.ktor.network.selector.SelectorManager
import io.ktor.network.sockets.aSocket
import io.ktor.server.cio.CIO
import io.ktor.server.engine.embeddedServer
import io.ktor.server.routing.routing
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.IO
import kotlinx.coroutines.Job
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.awaitCancellation
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeout
import platform.Foundation.NSNetService
import platform.Foundation.NSNotificationCenter
import platform.Foundation.NSOperationQueue
import platform.Foundation.NSTemporaryDirectory
import platform.UIKit.UIApplicationWillEnterForegroundNotification

/**
 * PC(Desktop) 업로드 수신 서버와 그 Bonjour 광고. Android 는 포그라운드 서비스가 붙잡아 두지만
 * iOS 에는 그런 장치가 없어 **앱이 서스펜드되면 함께 멈춘다.**
 */
internal class IosFileShareServerController(
    private val importUploadedAudioMedia: ImportUploadedAudioMediaUseCase,
    private val validateUploadedAudioFileName: ValidateUploadedAudioFileNameUseCase,
    private val updateAudioMedia: UpdateAudioMediaUseCase,
    private val saveAudioMediaImage: SaveAudioMediaImageUseCase,
) : FileShareServerController {

    // NSNetService 는 만든 스레드의 런루프에서 광고한다.
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main)

    private val _isRunning = MutableStateFlow(false)
    override val isRunning: StateFlow<Boolean> = _isRunning.asStateFlow()

    private var serving: Job? = null

    init {
        // 서스펜드된 동안 OS 가 리슨 소켓을 회수할 수 있다. 회수되면 오류 없이 접속만 거절된다.
        NSNotificationCenter.defaultCenter.addObserverForName(
            name = UIApplicationWillEnterForegroundNotification,
            `object` = null,
            queue = NSOperationQueue.mainQueue,
        ) { _ ->
            if (_isRunning.value) {
                scope.launch { if (_isRunning.value && !isListening()) launchServing() }
            }
        }
    }

    override fun start() {
        if (_isRunning.value) return
        _isRunning.value = true
        launchServing()
    }

    override fun stop() {
        _isRunning.value = false
        serving?.cancel()
    }

    // 앞 서버가 포트를 놓기 전에 새로 띄우면 바인드에 실패한다.
    private fun launchServing() {
        val previous = serving
        serving = scope.launch {
            previous?.cancelAndJoin()
            serve()
        }
    }

    private suspend fun serve() {
        val server = try {
            embeddedServer(CIO, port = FileShareConstants.DEFAULT_PORT) {
                installFileShareContentNegotiation()
                routing {
                    pcUploadEndpoints(
                        importUploadedAudioMedia = importUploadedAudioMedia,
                        validateUploadedAudioFileName = validateUploadedAudioFileName,
                        updateAudioMedia = updateAudioMedia,
                        saveAudioMediaImage = saveAudioMediaImage,
                        uploadTempDir = NSTemporaryDirectory(),
                        onUploadImported = { result ->
                            IosLocalNotification.show(
                                WorkResultNotice.Download.Success(
                                    artist = result.audioMedia.artist,
                                    name = result.audioMedia.name,
                                    playlistId = result.playlistIdOnDownload,
                                    audioMediaId = result.audioMedia.id,
                                ),
                            )
                        },
                    )
                }
            }.startSuspend(wait = false)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Throwable) {
            _isRunning.value = false
            return
        }
        val advertisement = NSNetService(
            domain = "",
            type = FileShareConstants.NSD_SERVICE_TYPE,
            name = FileShareConstants.NSD_SERVICE_NAME,
            port = FileShareConstants.DEFAULT_PORT,
        ).apply { publish() }

        try {
            awaitCancellation()
        } finally {
            advertisement.stop()
            withContext(NonCancellable) { server.stopSuspend(STOP_GRACE_MS, STOP_TIMEOUT_MS) }
        }
    }

    private suspend fun isListening(): Boolean = runCatching {
        SelectorManager(Dispatchers.IO).use { selector ->
            withTimeout(PROBE_TIMEOUT_MS) {
                aSocket(selector).tcp().connect(LOOPBACK_HOST, FileShareConstants.DEFAULT_PORT).close()
            }
        }
    }.isSuccess

    private companion object {
        const val STOP_GRACE_MS = 500L
        const val STOP_TIMEOUT_MS = 500L
        const val PROBE_TIMEOUT_MS = 1_000L
        const val LOOPBACK_HOST = "127.0.0.1"
    }
}
