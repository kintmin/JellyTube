package com.kintmin.domain.playlist_download.model

import com.kintmin.domain.youtube.model.YoutubePlaylist

data class PlaylistDownloadPreview(
    val playlist: YoutubePlaylist,
    val totalCount: Int,
    val alreadyOnDeviceCount: Int,
    val downloadCount: Int,
    val unavailableCount: Int,
    val estimatedDownloadBytes: Long,
    /** 같은 이름의 재생목록이 이미 있어 거기에 합친다. */
    val mergesIntoExistingPlaylist: Boolean,
)
