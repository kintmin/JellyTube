package com.kintmin.ytmusicbox.data.remote.dto

data class YoutubeDownloadDto(
    val title: String?,
    val description: String?,
    val rawData: ByteArray,
)