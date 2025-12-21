package com.kintmin.domain.audio_media.model

data class DownloadedMedia(
    val downloadUrl: String,
    val title: String,
    val audioFileNameWithExt: String,
    val imageFileNameWithExt: String?,
    val duration: String,
    val uploader: String,
    val description: String,
)