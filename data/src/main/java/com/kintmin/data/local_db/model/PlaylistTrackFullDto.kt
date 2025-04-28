package com.kintmin.data.local_db.model

import androidx.room.Embedded
import androidx.room.Relation

data class PlaylistTrackFullDto(
    @Embedded val playlistTrackEntity: PlaylistTrackEntity,

    @Relation(
        parentColumn = "playlistId",
        entityColumn = "id"
    )
    val playlistEntity: PlaylistEntity,

    @Relation(
        parentColumn = "audioMediaId",
        entityColumn = "id"
    )
    val audioMediaEntity: AudioMediaEntity,
)