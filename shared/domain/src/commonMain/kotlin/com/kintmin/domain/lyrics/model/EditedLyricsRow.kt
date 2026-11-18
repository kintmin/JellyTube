package com.kintmin.domain.lyrics.model

/** 가사 편집 화면의 한 줄. 번역·음차는 원본과 같은 타임코드로 저장한다. */
data class EditedLyricsRow(
    val timeMs: Long,
    val text: String,
    val translation: String,
    val transliteration: String,
)
