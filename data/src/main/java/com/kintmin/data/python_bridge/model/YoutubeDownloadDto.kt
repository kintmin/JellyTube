package com.kintmin.data.python_bridge.model

data class YoutubeDownloadDto(
    val title: String,
    val thumbnailDownloadUrl: String,
    val duration: String,
    val uploader: String,
    val description: String,
)