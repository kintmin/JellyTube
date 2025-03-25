package com.kintmin.localdatabase.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "AUDIO_MEDIA",
    indices = [
        Index("playlistId"),
    ],
    foreignKeys = [
        ForeignKey(
            entity = PlaylistEntity::class,
            parentColumns = ["id"],
            childColumns = ["playlistId"]
        )
    ]
)
data class AudioMediaEntity(
    @PrimaryKey val id: String,
    val playlistId: Int? = null,
    val artist: String,
    val mediaName: String,
    val description: String = "",
    val rawAudioDuration: Long? = null,
    val rawCreatedTime: Long,
    val audioFileNameWithExt: String,
    val imageFileNameWithExt: String?,
)
