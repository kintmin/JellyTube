package com.kintmin.data.file_share

import com.kintmin.fileshare.FileShareConstants
import com.kintmin.log.AppLog
import com.kintmin.log.model.DebugLog
import io.ktor.server.application.Application
import io.ktor.server.cio.CIO
import io.ktor.server.engine.embeddedServer
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.awaitCancellation
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/** 받는 서버 한 벌. 플랫폼은 언제 켜고 끌지와 무엇을 광고할지만 정한다. */
internal class FileShareServer(
    private val scope: CoroutineScope,
    private val appLog: AppLog,
    private val module: Application.() -> Unit,
) {
    private var serving: Job? = null

    /** 떠 있으면 내리고 다시 띄운다. [whileServing] 은 서버가 떠 있는 동안 돈다. */
    fun launch(onStartFailed: () -> Unit = {}, whileServing: suspend () -> Unit = {}) {
        val previous = serving
        serving = scope.launch {
            // 앞 서버가 포트를 놓기 전에 새로 띄우면 바인드에 실패한다.
            previous?.cancelAndJoin()
            serve(onStartFailed, whileServing)
        }
    }

    fun stop() {
        serving?.cancel()
        serving = null
    }

    private suspend fun serve(onStartFailed: () -> Unit, whileServing: suspend () -> Unit) {
        val server = embeddedServer(CIO, port = FileShareConstants.DEFAULT_PORT, module = module)
        try {
            server.startSuspend(wait = false)
        } catch (e: Throwable) {
            // 바인드까지 끝낸 뒤 취소·실패하면 엔진이 포트를 쥔 채 남아 다음 시작이 실패한다.
            withContext(NonCancellable) { runCatching { server.stopSuspend(0, 0) } }
            if (e is CancellationException) throw e
            appLog.sendDebugLog(DebugLog(LOG_TAG, "파일 공유 서버를 띄우지 못했습니다: ${e.message}"))
            onStartFailed()
            return
        }
        try {
            whileServing()
            awaitCancellation()
        } finally {
            withContext(NonCancellable) { server.stopSuspend(STOP_GRACE_MS, STOP_TIMEOUT_MS) }
        }
    }

    private companion object {
        const val LOG_TAG = "FileShareServer"
        const val STOP_GRACE_MS = 500L
        const val STOP_TIMEOUT_MS = 500L
    }
}
