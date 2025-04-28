package com.kintmin.data.local_db.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "AUDIO_MEDIA")
data class AudioMediaEntity(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val source: String, // 원본 파일의 출처이자 파일 이름 (유튜브: 영상 id)
    val mediaName: String,
    val artist: String,
    val description: String,
    val rawAudioDurationSeconds: Long? = null,
    val audioFileExt: String,
    val imageFileExt: String? = null,
    val rawCreatedTime: Long,
)
