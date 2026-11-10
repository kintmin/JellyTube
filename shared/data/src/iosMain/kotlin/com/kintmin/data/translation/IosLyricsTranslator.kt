package com.kintmin.data.translation

/**
 * iOS 가사 번역 미구현 스텁. ML Kit 대응물이 Apple `Translation`(iOS 18+ — 배포 타깃 17.0 과
 * 충돌)이나 ML Kit iOS SDK 뿐이다.
 *
 * `LyricsTranslationRepositoryImpl` 이 이것을 생성자로 받으므로 **바인딩을 비워 두면**
 * 가사 번역 Repository 를 꺼내는 순간 Koin 이 터진다.
 */
internal class IosLyricsTranslator : LyricsTranslator {

    override suspend fun translateToKorean(text: String, sourceLanguage: String): Result<String> =
        Result.failure(UnsupportedOperationException("iOS 에서는 가사 번역을 지원하지 않습니다."))
}
