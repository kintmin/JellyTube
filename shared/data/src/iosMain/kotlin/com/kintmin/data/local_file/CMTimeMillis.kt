package com.kintmin.data.local_file

import kotlinx.cinterop.CValue
import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.cinterop.useContents
import platform.CoreMedia.CMTime

/**
 * `CMTime` 을 밀리초로. 길이를 못 읽으면 null 이다.
 *
 * `CMTimeGetSeconds` 를 쓰지 않는다 — 구조체를 값으로 넘기는 호출이라 Kotlin/Native 에서
 * 값이 어긋난다.
 */
@OptIn(ExperimentalForeignApi::class)
internal fun CValue<CMTime>.toMillisOrNull(): Long? = useContents {
    if (timescale <= 0) return@useContents null

    val seconds = value.toDouble() / timescale.toDouble()
    if (!seconds.isFinite() || seconds <= 0.0) null else (seconds * 1000.0).toLong()
}
