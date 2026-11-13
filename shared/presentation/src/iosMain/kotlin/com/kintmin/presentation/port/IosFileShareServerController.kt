package com.kintmin.presentation.port

import com.kintmin.domain.audio_media.usecase.ImportUploadedAudioMediaUseCase
import com.kintmin.domain.audio_media.usecase.SaveAudioMediaImageUseCase
import com.kintmin.domain.audio_media.usecase.UpdateAudioMediaUseCase
import com.kintmin.domain.audio_media.usecase.ValidateUploadedAudioFileNameUseCase
import com.kintmin.fileshare.FileShareConstants
import com.kintmin.log.AppLog
import com.kintmin.log.model.DebugLog
import com.kintmin.presentation.platform.ScreenAwake
import io.ktor.network.selector.SelectorManager
import io.ktor.network.sockets.aSocket
import io.ktor.server.cio.CIO
import io.ktor.server.engine.embeddedServer
import io.ktor.server.routing.routing
import kotlinx.cinterop.ObjCSignatureOverride
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.IO
import kotlinx.coroutines.Job
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.awaitCancellation
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeout
import platform.Foundation.NSNetService
import platform.Foundation.NSNetServiceDelegateProtocol
import platform.Foundation.NSNotificationCenter
import platform.Foundation.NSOperationQueue
import platform.Foundation.NSTemporaryDirectory
import platform.UIKit.UIApplicationWillEnterForegroundNotification
import platform.darwin.NSObject

/** PC 업로드 수신 서버와 그 Bonjour 광고. 앱이 서스펜드되면 함께 멈춘다. */
internal class IosFileShareServerController(
    private val importUploadedAudioMedia: ImportUploadedAudioMediaUseCase,
    private val validateUploadedAudioFileName: ValidateUploadedAudioFileNameUseCase,
    private val updateAudioMedia: UpdateAudioMediaUseCase,
    private val saveAudioMediaImage: SaveAudioMediaImageUseCase,
    private val appLog: AppLog,
) : FileShareServerController {

    // NSNetService 는 만든 스레드의 런루프에서 광고한다.
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main)

    private val _isRunning = MutableStateFlow(false)
    override val isRunning: StateFlow<Boolean> = _isRunning.asStateFlow()

    private var serving: Job? = null
    private var advertisement: BonjourAdvertisement? = null

    init {
        // 수신 화면이 아니라 서버에 묶는다. 다른 화면에서도 자동 잠금이 오면 서스펜드되어 서버가 멈춘다.
        scope.launch { ScreenAwake.holdWhile(_isRunning) }

        // 서스펜드된 동안 OS 가 리슨 소켓을 회수하거나 광고를 내릴 수 있다. 둘 다 오류 없이 조용히 사라진다.
        NSNotificationCenter.defaultCenter.addObserverForName(
            name = UIApplicationWillEnterForegroundNotification,
            `object` = null,
            queue = NSOperationQueue.mainQueue,
        ) { _ ->
            if (_isRunning.value) {
                scope.launch {
                    if (!_isRunning.value) return@launch
                    if (isListening()) advertisement?.publish() else launchServing()
                }
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
        val server = embeddedServer(CIO, port = FileShareConstants.DEFAULT_PORT) {
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
        }
        try {
            server.startSuspend(wait = false)
        } catch (e: Throwable) {
            // 바인드까지 끝낸 뒤 취소·실패하면 엔진이 포트를 쥔 채 남아 다음 시작이 실패한다.
            withContext(NonCancellable) { runCatching { server.stopSuspend(0, 0) } }
            if (e is CancellationException) throw e
            appLog.sendDebugLog(DebugLog(LOG_TAG, "PC 수신 서버를 띄우지 못했습니다: ${e.message}"))
            _isRunning.value = false
            return
        }

        val bonjour = BonjourAdvertisement(scope, appLog).also { advertisement = it }
        bonjour.publish()
        try {
            awaitCancellation()
        } finally {
            if (advertisement === bonjour) advertisement = null
            bonjour.stop()
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
        const val LOG_TAG = "IosFileShareServerController"
        const val STOP_GRACE_MS = 500L
        const val STOP_TIMEOUT_MS = 500L
        const val PROBE_TIMEOUT_MS = 1_000L
        const val LOOPBACK_HOST = "127.0.0.1"
    }
}

/** 등록이 실패해도 서버는 멀쩡해 보여 Desktop 이 찾지 못한다. 몇 번 다시 등록한다. */
private class BonjourAdvertisement(
    private val scope: CoroutineScope,
    private val appLog: AppLog,
) : NSObject(), NSNetServiceDelegateProtocol {
    private var service: NSNetService? = null
    private var retryCount = 0
    private var retryJob: Job? = null

    fun publish() {
        retryCount = 0
        register()
    }

    fun stop() {
        retryJob?.cancel()
        unregister()
    }

    private fun register() {
        unregister()
        service = NSNetService(
            domain = "",
            type = FileShareConstants.NSD_SERVICE_TYPE,
            name = FileShareConstants.NSD_SERVICE_NAME,
            port = FileShareConstants.DEFAULT_PORT,
        ).also {
            it.delegate = this
            it.publish()
        }
    }

    // delegate 는 강하게 잡히지 않는다. 이 객체가 먼저 사라져도 불리지 않게 끊는다.
    private fun unregister() {
        service?.let {
            it.delegate = null
            it.stop()
        }
        service = null
    }

    @ObjCSignatureOverride
    override fun netService(sender: NSNetService, didNotPublish: Map<Any?, *>) {
        if (sender !== service) return
        appLog.sendDebugLog(DebugLog(BONJOUR_LOG_TAG, "Bonjour 광고 등록 실패($retryCount): $didNotPublish"))
        if (retryCount >= BONJOUR_MAX_RETRIES) return
        retryCount++
        retryJob?.cancel()
        retryJob = scope.launch {
            delay(BONJOUR_RETRY_DELAY_MS)
            register()
        }
    }
}

private const val BONJOUR_LOG_TAG = "BonjourAdvertisement"
private const val BONJOUR_MAX_RETRIES = 3
private const val BONJOUR_RETRY_DELAY_MS = 2_000L
