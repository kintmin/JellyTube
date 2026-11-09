package com.kintmin.data.python_bridge

/**
 * iOS 가사 음차 미구현 스텁.
 *
 * 음차 스크립트(`transliterate_lyrics.py`)는 iOS 번들에 이미 들어가 있지만 그것이 쓰는
 * `pykakasi` 가 `ios/app/Vendor/Python/app-packages` 에 벤더링되어 있지 않다. 패키지를 넣고
 * `IosPythonExecutorBridge` 에 음차 함수를 더하면 이 스텁은 사라진다.
 *
 * `LyricsTranslationRepositoryImpl` 이 광범위 single 로 등록되므로 바인딩을 비워 두면
 * iOS 에서 가사 번역 Repository 를 꺼내는 순간 Koin 이 터진다. 그래서 스텁을 둔다.
 */
internal class IosLyricsTransliterator : LyricsTransliterator {

    override suspend fun transliterateToKorean(text: String): Result<String> =
        Result.failure(UnsupportedOperationException("iOS 에서는 가사 음차를 지원하지 않습니다."))
}
