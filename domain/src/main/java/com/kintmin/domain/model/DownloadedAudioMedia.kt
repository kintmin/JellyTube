package com.kintmin.domain.model

import java.time.LocalDateTime

data class DownloadedAudioMedia(
    val source: String,
    val title: String,
    val duration: Long?,
    val uploader: String,
    val description: String,
    val createdTime: LocalDateTime,
    val audioFileNameWithExt: String,
    val imageFileNameWithExt: String?,
)