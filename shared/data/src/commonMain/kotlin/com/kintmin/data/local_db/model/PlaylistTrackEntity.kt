package com.kintmin.data.local_db.model

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import kotlinx.datetime.Clock

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
    val sequence: Int,  // 1부터 시작하며, 중복 없이 증가하는 값. 트랙 추가/삭제에 따라 순차가 아닐 수도 있다.
    val rawCreatedTime: Long = Clock.System.now().toEpochMilliseconds(),
)
