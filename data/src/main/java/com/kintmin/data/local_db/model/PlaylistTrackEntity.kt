package com.kintmin.data.local_db.model

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index

@Entity(
    tableName = "PLAYLIST_TRACK",
    primaryKeys = ["playlistId", "audioMediaId"],
    foreignKeys = [
        ForeignKey(
            entity = AudioMediaEntity::class,
            parentColumns = ["id"],
            childColumns = ["audioMediaId"]
        ),
        ForeignKey(
            entity = PlaylistEntity::class,
            parentColumns = ["id"],
            childColumns = ["playlistId"]
        ),
    ],
    indices = [
        Index(value = ["playlistId", "sequence"]),
        Index(value = ["audioMediaId"]),
    ],
)
data class PlaylistTrackEntity(
    val playlistId: Int,
    val audioMediaId: Int,
    val sequence: Int,
    val rawCreatedTime: Long,
)