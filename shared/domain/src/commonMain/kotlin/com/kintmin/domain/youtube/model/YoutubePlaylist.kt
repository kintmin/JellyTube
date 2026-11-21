package com.kintmin.domain.youtube.model

/** [entries] 는 유튜브 재생목록의 순서다. */
data class YoutubePlaylist(
    val title: String,
    val entries: List<YoutubePlaylistEntry>,
)
