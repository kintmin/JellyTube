package com.kintmin.data.local_db.model

import androidx.room.Entity
import androidx.room.PrimaryKey
import kotlin.time.Clock

/**
 * id와 source가 고유한 Entity.
 */
@Entity(tableName = "AUDIO_MEDIA")
data class AudioMediaEntity(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val source: String,
    val name: String,
    val artist: String,
    val description: String,
    val rawAudioDurationSeconds: Long? = null,
    val audioFileNameWithExt: String,
    val imageFileNameWithExt: String? = null,
    val lyricFileNameWithExt: String? = null,
    val tjKaraokeNumber: String? = null,
    /**
     * 편집본 파일명. null이면 편집본이 없다는 뜻이고 원본([audioFileNameWithExt])을 그대로 쓴다.
     * 원본 파일은 편집 후에도 지우지 않아 언제든 되돌릴 수 있다.
     */
    val editedAudioFileNameWithExt: String? = null,
    /** 편집본 길이(초). [editedAudioFileNameWithExt]와 항상 같이 채우고 같이 비운다. */
    val editedAudioDurationSeconds: Long? = null,
    val rawCreatedTime: Long = Clock.System.now().toEpochMilliseconds(),
)
