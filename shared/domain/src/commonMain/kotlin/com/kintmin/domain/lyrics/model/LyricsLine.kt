package com.kintmin.domain.lyrics.model

/**
 * 가사 한 줄. timeMs 가 null 이면 비싱크(일반) 가사 줄이다.
 */
data class LyricsLine(
    val timeMs: Long?,
    val text: String,
)
