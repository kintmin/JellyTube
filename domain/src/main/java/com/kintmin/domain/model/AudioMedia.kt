package com.kintmin.domain.model

import java.time.LocalDateTime
import kotlin.time.Duration

data class AudioMedia(
    val id: Int,
    val playlistId: Int,
    val audioMediaSequence: Int,
    val sourcePath: String,
    val mediaName: String,
    val artist: String,
    val description: String,
    val audioDuration: Duration?,
    val createdTime: LocalDateTime,
    val audioFileFullPath: String,
    val imageFileFullPath: String?,
)
