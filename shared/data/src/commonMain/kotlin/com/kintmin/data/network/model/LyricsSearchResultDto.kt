package com.kintmin.data.network.model

import kotlinx.serialization.Serializable

@Serializable
data class LyricsSearchResultDto(
    val id: Int,
    val trackName: String? = null,
    val artistName: String? = null,
    val albumName: String? = null,
    val duration: Double? = null,
    val instrumental: Boolean = false,
    val plainLyrics: String? = null,
    val syncedLyrics: String? = null,
)
