package com.kintmin.domain.model

import java.time.LocalDateTime
import kotlin.time.Duration

data class AudioMedia(
    val id: String,
    val playlistId: Int?,
    val mediaName: String,
    val description: String,
    val artist: String,
    val audioDuration: Duration?,
    val createdTime: LocalDateTime,
    val audioFileFullPath: String?, // null일 경우 오디오 파일에 문제
    val imageFileFullPath: String?,
)
