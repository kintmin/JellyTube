package com.kintmin.data.python_bridge

interface LyricsTransliterator {
    /**
     * 일본어 등 외국어 가사 텍스트를 한국어 발음(한글 음차)으로 변환한다.
     * 줄 구조는 그대로 보존한다. (예: "君の名は" -> "기미노 나마에")
     */
    suspend fun transliterateToKorean(text: String): Result<String>
}
