package com.kintmin.log

/**
 * 제한1: 이벤트명의 최대 길이는 영문 기준 40자
 * 제한2: 한 로그 당 params 최대 개수는 25개
 * 제한3: 앱 사용자당 500개
 * https://support.google.com/analytics/answer/9267744
 */
sealed class FirebaseEvent(rawLogName: String, vararg rawParams: Pair<String, Any?>) {

    val logName = rawLogName.take(40)
    val params = rawParams.take(25).toTypedArray()

    data class SuccessRegisterUser(val userId: String) : FirebaseEvent(
        "SuccessRegisterUser",
        FirebaseParam.userId(userId),
    )

    data class FailedRegisterUser(val exception: Throwable) : FirebaseEvent(
        "FailedRegisterUser",
        FirebaseParam.errorMessage(exception),
    )

    data class FailedDownloadAudioMedia(
        val url: String,
        val exception: Throwable,
        val availableRemMemory: Long? = null,
        val isLowRemMemory: Boolean? = null,
        val availableStorage: Long? = null,
        val isConnected: Boolean? = null,
        val isWifi: Boolean? = null,
        val isCellular: Boolean? = null,
        val downstreamKbps: Int? = null,
        val upstreamKbps: Int? = null,
    ) : FirebaseEvent(
        "FailedDownloadAudioMedia",
        FirebaseParam.url(url),
        FirebaseParam.errorMessage(exception),
        FirebaseParam.availableRemMemory(availableRemMemory),
        FirebaseParam.isLowRemMemory(isLowRemMemory),
        FirebaseParam.availableStorage(availableStorage),
        FirebaseParam.isConnected(isConnected),
        FirebaseParam.isWifi(isWifi),
        FirebaseParam.isCellular(isCellular),
        FirebaseParam.downstreamKbps(downstreamKbps),
        FirebaseParam.upstreamKbps(upstreamKbps),
    )
}