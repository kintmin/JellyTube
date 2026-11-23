package com.kintmin.data.python_bridge

import com.kintmin.domain.common.suspendRunCatching

/**
 * Swift 가 들고 있는 Python 런타임에 음차를 위임한다.
 * iOS 쪽 `pykakasi` 패키지는 `scripts/setup-ios-vendor.sh` 가 채운다.
 */
internal class IosLyricsTransliterator(
    private val bridge: IosPythonExecutorBridge,
) : LyricsTransliterator {

    override suspend fun transliterateToKorean(text: String): Result<String> = suspendRunCatching {
        bridge.transliterateToKorean(text)
    }
}
