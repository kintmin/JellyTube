package com.kintmin.ytmusicbox.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "media")
data class YoutubeMediaEntity(
    @PrimaryKey val id: String,
    val title: String,
    val description: String,
    val audioFilePath: String,
    val imageFilePath: String?,
)
