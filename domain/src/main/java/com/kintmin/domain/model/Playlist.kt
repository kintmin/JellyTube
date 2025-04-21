package com.kintmin.domain.model

import kotlin.time.Duration

data class Playlist(
    val id: Long,
    val name: String,
    val totalDuration: Duration,
)
