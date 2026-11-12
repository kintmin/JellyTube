package com.kintmin.data.translation

/**
 * Apple `Translation` 은 Swift 전용이라 Swift 가 구현한다. 세션을 SwiftUI `.translationTask`
 * 로만 얻을 수 있어서 Kotlin/Native 인터롭으로는 부를 수 없다.
 */
interface IosLyricsTranslatorBridge {
    /**
     * [lines] 를 한 줄씩 한국어로 번역한다. 결과는 [lines] 와 같은 순서·같은 개수다.
     * 줄 나누기와 빈 줄 처리는 Kotlin 이 하고 여기에는 번역할 줄만 넘어온다.
     *
     * @param sourceLanguage BCP-47 언어 코드 (예: "en", "ja")
     */
    suspend fun translateToKorean(lines: List<String>, sourceLanguage: String): List<String>
}
