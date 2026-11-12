package com.kintmin.platform.service.migration

import com.kintmin.fileshare.MigrationFinishRequest
import java.security.MessageDigest
import java.security.SecureRandom

/**
 * 데이터 이전 한 번의 인가 상태. 화면에 띄운 [code] 를 맞힌 기기 하나에만 토큰을 준다.
 * 토큰을 주소에 묶지 않는다. 받는 기기의 IP 가 바뀌어도 같은 토큰으로 이어 받는다.
 *
 * 코드를 [MAX_FAILED_ATTEMPTS] 번 틀리면 세션이 닫힌다. 6자리는 무차별 대입에 약해서
 * 시도 횟수로 막는다. 닫힌 세션은 다시 열 수 없다 — 새 세션(새 코드)을 만든다.
 */
class MigrationSession(
    val code: String = generateCode(),
    private val generateToken: () -> String = ::generateToken,
    private val onClosed: () -> Unit = {},
    private val onPaired: (deviceName: String) -> Unit = {},
    private val onFinished: (MigrationFinishRequest) -> Unit = {},
) {
    private var token: String? = null
    private var failedAttempts = 0
    private var allowedFiles: Set<Pair<String, String>> = emptySet()

    @get:Synchronized
    var isClosed = false
        private set

    /**
     * 코드를 모르면 페어링 여부와 관계없이 [PairResult.Rejected] 다. 코드 없이 이전 중인지 엿볼 수 없다.
     * 페어링한 뒤의 틀린 코드는 잠금으로 세지 않는다. 받고 있는 기기를 끊지 않는다.
     */
    fun pair(code: String, deviceName: String): PairResult {
        var hasLockedOut = false
        val result = synchronized(this) {
            val isCodeCorrect = constantTimeEquals(code, this.code)
            when {
                isClosed -> PairResult.Rejected
                token != null -> if (isCodeCorrect) PairResult.Busy else PairResult.Rejected
                !isCodeCorrect -> {
                    failedAttempts++
                    if (failedAttempts >= MAX_FAILED_ATTEMPTS) hasLockedOut = markClosed()
                    PairResult.Rejected
                }
                else -> PairResult.Issued(generateToken().also { token = it })
            }
        }
        if (hasLockedOut) onClosed()
        if (result is PairResult.Issued) onPaired(toPeerName(deviceName))
        return result
    }

    @Synchronized
    fun isAuthorized(token: String?): Boolean {
        val issued = this.token
        return !isClosed && issued != null && token != null && constantTimeEquals(token, issued)
    }

    /** manifest 로 내려준 파일만 받아 갈 수 있다. */
    @Synchronized
    fun allowFiles(files: Set<Pair<String, String>>) {
        allowedFiles = files
    }

    @Synchronized
    fun isFileAllowed(kind: String, name: String): Boolean = kind to name in allowedFiles

    /** 받는 기기가 끝났다고 알렸다. 실패한 곡을 다시 받을 수 있도록 세션은 닫지 않는다. */
    fun finish(report: MigrationFinishRequest) {
        if (isClosed) return
        onFinished(report)
    }

    // 콜백은 잠금 밖에서 부른다. 받는 쪽이 자기 잠금을 쥔 채 close() 를 부르면 교착된다.
    fun close() {
        if (markClosed()) onClosed()
    }

    @Synchronized
    private fun markClosed(): Boolean {
        if (isClosed) return false
        isClosed = true
        return true
    }

    companion object {
        const val MAX_FAILED_ATTEMPTS = 5

        private val secureRandom = SecureRandom()

        private fun generateCode(): String = secureRandom.nextInt(1_000_000).toString().padStart(6, '0')

        @OptIn(ExperimentalStdlibApi::class)
        private fun generateToken(): String = ByteArray(32).also(secureRandom::nextBytes).toHexString()

        /** 받는 기기가 보낸 이름이라 보내는 화면에 그대로 띄우지 않는다. */
        internal fun toPeerName(deviceName: String): String {
            val cleaned = deviceName.filterNot { it.isISOControl() || it.isWhitespace() && it != ' ' }.trim()
            if (cleaned.isEmpty()) return UNKNOWN_PEER_NAME
            val end = if (cleaned.codePointCount(0, cleaned.length) > MAX_PEER_NAME_CODE_POINTS) {
                cleaned.offsetByCodePoints(0, MAX_PEER_NAME_CODE_POINTS)
            } else {
                cleaned.length
            }
            return cleaned.substring(0, end).trimEnd()
        }

        private const val MAX_PEER_NAME_CODE_POINTS = 40
        private const val UNKNOWN_PEER_NAME = "이름 없는 기기"

        private fun constantTimeEquals(a: String, b: String): Boolean =
            MessageDigest.isEqual(a.toByteArray(), b.toByteArray())
    }
}

sealed interface PairResult {
    data class Issued(val token: String) : PairResult
    data object Rejected : PairResult
    data object Busy : PairResult
}
