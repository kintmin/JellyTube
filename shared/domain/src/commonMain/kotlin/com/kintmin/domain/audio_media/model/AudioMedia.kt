package com.kintmin.domain.audio_media.model

import kotlinx.datetime.LocalDateTime
import kotlin.time.Duration

data class AudioMedia(
    val id: Int,
    val source: String,
    val name: String,
    val artist: String,
    val description: String,
    val audioDuration: Duration?,
    val audioFileFullPath: String,
    val imageFileFullPath: String?,
    val createdTime: LocalDateTime,
)
