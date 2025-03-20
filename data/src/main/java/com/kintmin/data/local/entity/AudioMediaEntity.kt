package com.kintmin.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "media")
data class AudioMediaEntity(
    @PrimaryKey val id: String,
    val title: String,
    val description: String,
    val audioFileNameWithExt: String,
    val imageFileNameWithExt: String?,
)
