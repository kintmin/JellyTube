package com.kintmin.data.translation

/** Apple `Translation` 세션은 SwiftUI `.translationTask` 로만 나와 Kotlin/Native 가 부를 수 없다. */
interface IosLyricsTranslatorBridge {
    /** 결과는 [lines] 와 같은 순서·같은 개수다. 번역할 수 없는 언어면 원문을 그대로 돌려준다. */
    suspend fun translateToKorean(lines: List<String>, sourceLanguage: String): List<String>
}
