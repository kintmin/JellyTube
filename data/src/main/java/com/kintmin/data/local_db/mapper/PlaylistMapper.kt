package com.kintmin.data.local_db.mapper

import com.kintmin.data.local_db.model.PlaylistEntity
import com.kintmin.domain.extension.toLocalDateTime
import com.kintmin.domain.model.Playlist
import kotlin.time.Duration.Companion.seconds

fun PlaylistEntity.toDomain() = Playlist(
    id = id,
    name = name,
    description = description,
    audioMediaCount = audioMediaCount,
    playTimeDuration = rawPlayTimeDuration.seconds,
    createdTime = rawCreatedTime.toLocalDateTime(),
)