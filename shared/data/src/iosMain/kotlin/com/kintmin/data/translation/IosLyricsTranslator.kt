package com.kintmin.data.translation

/**
 * iOS 가사 번역 미구현 스텁.
 *
 * Android 는 ML Kit 온디바이스 번역을 쓴다. iOS 대응물은 Apple `Translation`
 * 프레임워크(iOS 18+ — 현 배포 타깃 17.0 과 충돌)나 ML Kit iOS SDK(CocoaPods 도입)뿐이라
 * 어느 쪽도 선행 결정이 필요하다.
 *
 * `LyricsTranslationRepositoryImpl` 이 광범위 single 로 등록되므로 바인딩을 비워 두면
 * iOS 에서 가사 번역 Repository 를 꺼내는 순간 Koin 이 터진다. 그래서 스텁을 둔다.
 */
internal class IosLyricsTranslator : LyricsTranslator {

    override suspend fun translateToKorean(text: String, sourceLanguage: String): Result<String> =
        Result.failure(UnsupportedOperationException("iOS 에서는 가사 번역을 지원하지 않습니다."))
}
