package com.kintmin.localdatabase.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "PLAYLIST")
data class PlaylistEntity(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val name: String,
    val totalPlayTime: Int,
)