package com.kintmin.domain.lyrics.model

/** 편집본 시간축으로 옮긴 가사 세 겹. 뜻과 읽는 법은 원본에 있을 때만 채워진다. */
data class EditedAudioLyrics(
    val synced: String,
    val translation: String?,
    val transliteration: String?,
)
