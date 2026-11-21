package com.kintmin.domain.playlist_download.model

data class PlaylistDownloadFailure(
    val title: String,
    val reason: PlaylistDownloadFailureReason,
)
