package com.kintmin.data.local_db.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "PLAYLIST")
data class PlaylistEntity(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val name: String,
    val description: String,
    val audioMediaCount: Int,
    val rawPlayTimeDuration: Long,
    val rawCreatedTime: Long,
)