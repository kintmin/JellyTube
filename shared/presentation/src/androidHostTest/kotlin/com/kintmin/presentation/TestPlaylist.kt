package com.kintmin.presentation

import com.kintmin.domain.playlist.model.Playlist
import com.kintmin.domain.playlist.model.PlaylistType
import kotlinx.datetime.LocalDateTime
import kotlin.time.Duration

fun testPlaylist(
    id: Int,
    type: PlaylistType,
    audioMediaCount: Int = 0,
    playTimeDuration: Duration = Duration.ZERO,
) = Playlist(
    id = id,
    name = "p$id",
    description = "",
    audioMediaCount = audioMediaCount,
    playTimeDuration = playTimeDuration,
    createdTime = LocalDateTime(2026, 1, 1, 0, 0),
    imageFileFullPath = null,
    isCustomImage = false,
    sequence = 0,
    type = type,
)
