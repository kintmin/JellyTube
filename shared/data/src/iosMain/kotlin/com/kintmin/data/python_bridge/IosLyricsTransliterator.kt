package com.kintmin.data.python_bridge

/**
 * Swift 가 들고 있는 Python 런타임에 음차를 위임한다. 스크립트(`transliterate_lyrics.py`)와
 * `pykakasi` 는 두 플랫폼이 공유하고, iOS 쪽 패키지는 `scripts/setup-ios-vendor.sh` 가 채운다.
 */
internal class IosLyricsTransliterator(
    private val bridge: IosPythonExecutorBridge,
) : LyricsTransliterator {

    override suspend fun transliterateToKorean(text: String): Result<String> = runCatching {
        bridge.transliterateToKorean(text)
    }
}
