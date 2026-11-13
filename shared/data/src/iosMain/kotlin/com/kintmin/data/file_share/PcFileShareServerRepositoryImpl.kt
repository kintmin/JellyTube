package com.kintmin.data.file_share

import com.kintmin.domain.file_share.PcUploadHandler
import com.kintmin.domain.file_share.ReceiveKeepAlive
import com.kintmin.domain.file_share.repository.PcFileShareServerRepository
import com.kintmin.fileshare.FileShareConstants
import com.kintmin.log.AppLog
import com.kintmin.log.model.DebugLog
import io.ktor.network.selector.SelectorManager
import io.ktor.network.sockets.aSocket
import io.ktor.server.routing.routing
import kotlinx.cinterop.ObjCSignatureOverride
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.IO
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.awaitCancellation
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeout
import platform.Foundation.NSNetService
import platform.Foundation.NSNetServiceDelegateProtocol
import platform.Foundation.NSNotificationCenter
import platform.Foundation.NSOperationQueue
import platform.Foundation.NSTemporaryDirectory
import platform.UIKit.UIApplicationWillEnterForegroundNotification
import platform.darwin.NSObject

/** 앱이 서스펜드되면 서버와 광고가 함께 멈춘다. */
internal class PcFileShareServerRepositoryImpl(
    private val appLog: AppLog,
    private val receiveKeepAlive: ReceiveKeepAlive,
) : PcFileShareServerRepository {

    // NSNetService 는 만든 스레드의 런루프에서 광고한다.
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main)

    private val _isRunning = MutableStateFlow(false)
    override val isRunning: StateFlow<Boolean> = _isRunning.asStateFlow()

    private var handler: PcUploadHandler? = null
    private var advertisement: BonjourAdvertisement? = null

    private val server = FileShareServer(scope, appLog) {
        installFileShareContentNegotiation()
        routing {
            pcUploadEndpoints(
                handler = checkNotNull(handler),
                uploadTempDir = NSTemporaryDirectory(),
                receiveKeepAlive = receiveKeepAlive,
            )
        }
    }

    init {
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

    override fun start(handler: PcUploadHandler) {
        if (_isRunning.value) return
        this.handler = handler
        _isRunning.value = true
        launchServing()
    }

    override fun stop() {
        _isRunning.value = false
        server.stop()
    }

    private fun launchServing() = server.launch(
        onStartFailed = { _isRunning.value = false },
        whileServing = {
            val bonjour = BonjourAdvertisement(scope, appLog).also { advertisement = it }
            bonjour.publish()
            try {
                awaitCancellation()
            } finally {
                if (advertisement === bonjour) advertisement = null
                bonjour.stop()
            }
        },
    )

    private suspend fun isListening(): Boolean = runCatching {
        SelectorManager(Dispatchers.IO).use { selector ->
            withTimeout(PROBE_TIMEOUT_MS) {
                aSocket(selector).tcp().connect(LOOPBACK_HOST, FileShareConstants.DEFAULT_PORT).close()
            }
        }
    }.isSuccess

    private companion object {
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
