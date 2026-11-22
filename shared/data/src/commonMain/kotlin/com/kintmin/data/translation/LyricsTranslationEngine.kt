package com.kintmin.data.translation

/** 줄 단위 언어 감지와 번역의 플랫폼 구현. 결과는 요청과 같은 순서·같은 개수다. */
internal interface LyricsTranslationEngine {

    /** 줄마다 BCP-47 언어 코드. 감지하지 못한 줄은 null 이다. */
    suspend fun detectLanguages(lines: List<String>): List<String?>

    /** [sourceLanguage] 를 번역하지 못하는 엔진이면 원문을 그대로 돌려준다. */
    suspend fun translateToKorean(lines: List<String>, sourceLanguage: String): List<String>
}
