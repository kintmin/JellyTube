package com.kintmin.domain.playlist_download.model

data class PlaylistDownloadSummary(
    /** 담을 곡이 하나도 없어 재생목록을 만들지 않았으면 null. */
    val playlistId: Int?,
    val playlistName: String,
    val downloadedCount: Int,
    val alreadyOnDeviceCount: Int,
    val failures: List<PlaylistDownloadFailure>,
)
