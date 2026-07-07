package com.kintmin.data.local_db.model

import androidx.room.Entity
import androidx.room.PrimaryKey
import kotlinx.datetime.Clock

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
    val rawCreatedTime: Long = Clock.System.now().toEpochMilliseconds(),
)
