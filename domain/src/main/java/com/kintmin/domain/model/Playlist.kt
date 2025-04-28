package com.kintmin.domain.model

import java.time.LocalDateTime
import kotlin.time.Duration

data class Playlist(
    val id: Int,
    val name: String,
    val description: String,
    val audioMediaCount: Int,
    val playTimeDuration: Duration,
    val createdTime: LocalDateTime,
) {
    companion object {
        const val TOTAL = 1
        const val UNCATEGORIZED = 2
    }
}

