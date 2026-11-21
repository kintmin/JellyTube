package com.kintmin.domain.playlist_download.model

data class PlaylistDownloadProgress(
    val stage: PlaylistDownloadStage,
    val doneCount: Int = 0,
    val totalCount: Int = 0,
)
