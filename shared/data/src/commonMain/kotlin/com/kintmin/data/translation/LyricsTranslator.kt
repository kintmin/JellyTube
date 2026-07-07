package com.kintmin.data.translation

interface LyricsTranslator {
    /**
     * 외국어 가사 텍스트를 한국어(의미)로 번역한다. 줄 구조는 그대로 보존한다.
     *
     * @param text 원문 가사
     * @param sourceLanguage BCP-47 언어 코드 (예: "en", "ja")
     */
    suspend fun translateToKorean(text: String, sourceLanguage: String): Result<String>
}
