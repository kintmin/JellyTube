package com.kintmin.domain.lyrics.model

/**
 * 가사 텍스트의 언어 판별 결과.
 * - hasKorean: 한글이 포함되어 있는지 (없으면 의미 번역 메뉴 노출)
 * - hasJapanese: 일본어(히라가나/가타카나)가 포함되어 있는지 (있으면 음차 번역 메뉴 노출)
 * - sourceLanguage: 의미 번역에 쓸 BCP-47 원문 언어 코드 (휴리스틱)
 */
data class LyricsLanguage(
    val hasKorean: Boolean,
    val hasJapanese: Boolean,
    val sourceLanguage: String,
)
