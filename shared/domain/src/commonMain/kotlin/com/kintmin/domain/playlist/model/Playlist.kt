package com.kintmin.domain.playlist.model

import kotlinx.datetime.LocalDateTime
import kotlin.time.Duration

data class Playlist(
    val id: Int,
    val name: String,
    val description: String,
    val audioMediaCount: Int,
    val playTimeDuration: Duration,
    val createdTime: LocalDateTime,
    val imageFileFullPath: String?,
    val isCustomImage: Boolean,
    val sequence: Int,
    val type: PlaylistType,
) {
    val isBasePlaylist: Boolean get() = type.isSystem
}
