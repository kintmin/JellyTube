package com.kintmin.domain.lyrics.repository

interface LyricsTranslationRepository {
    /** 외국어 가사 텍스트를 한국어(의미)로 번역한다. 줄 구조는 보존한다. */
    suspend fun translate(text: String, sourceLanguage: String): Result<String>

    /** 외국어 가사 텍스트를 한글 음차(발음)로 변환한다. 줄 구조는 보존한다. */
    suspend fun transliterate(text: String): Result<String>
}
