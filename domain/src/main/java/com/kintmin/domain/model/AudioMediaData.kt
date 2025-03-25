package com.kintmin.domain.model

import java.time.LocalDateTime
import kotlin.time.Duration

data class AudioMediaData(
    val id: String,
    val playlistId: Int?,
    val mediaName: String,
    val description: String,
    val artist: String,
    val audioDuration: Duration?,
    val createdTime: LocalDateTime,
    val audioFileFullPath: String,
    val imageFileFullPath: String?,
)
