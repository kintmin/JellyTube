package com.kintmin.domain.playlist.model

/** @param targetId 저장된 대상이 고를 수 없는 곳이거나 없으면 미분류. 미분류도 아직 없으면 null. */
data class DownloadTargetPlaylists(
    val targetId: Int?,
    val selectable: List<Playlist>,
)
