package com.kintmin.data.local_db.model

import androidx.room.Entity
import androidx.room.PrimaryKey
import kotlinx.datetime.Clock

/**
 * 기본값 설정 시 DB에 addCallback 설정 필요
 */
@Entity(tableName = "PLAYLIST")
data class PlaylistEntity(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val name: String,
    val description: String,
    val audioMediaCount: Int,
    val rawPlayTimeDuration: Long,
    val rawCreatedTime: Long = Clock.System.now().toEpochMilliseconds(),
    val imageFileNameWithExt: String? = null,
    val isCustomImage: Boolean,
)
