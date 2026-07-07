package com.kintmin.domain.lyrics.model

/**
 * 원본 가사에서 파생되는 변형 가사 종류.
 * - TRANSLATION: 의미 번역(한국어)
 * - TRANSLITERATION: 음차 번역(한글 발음)
 */
enum class LyricsVariant {
    TRANSLATION,
    TRANSLITERATION,
}
