package com.kintmin.domain.lyrics.model

/**
 * 재생에 맞춰 보여 줄 가사.
 *
 * [lyricFileFullPath] 는 뜻과 읽는 법을 파생시킬 기준 경로다 — 줄 번호로 짝지으므로 보여 주는
 * 가사와 같은 경로에서 읽어야 한다.
 */
data class PlaybackLyrics(
    val lyricFileFullPath: String,
    val lines: List<LyricsLine>,
)
