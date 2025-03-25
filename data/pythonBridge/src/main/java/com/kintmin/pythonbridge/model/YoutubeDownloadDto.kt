package com.kintmin.pythonbridge.model

data class YoutubeDownloadDto(
    val title: String,
    val thumbnailDownloadUrl: String,
    val duration: String,
    val uploader: String,
    val description: String,
)