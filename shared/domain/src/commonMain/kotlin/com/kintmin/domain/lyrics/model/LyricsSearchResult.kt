package com.kintmin.domain.lyrics.model

data class LyricsSearchResult(
    val id: Int,
    val trackName: String?,
    val artistName: String?,
    val albumName: String?,
    val duration: Double?,
    val plainLyrics: String?,
    val syncedLyrics: String?,
)
