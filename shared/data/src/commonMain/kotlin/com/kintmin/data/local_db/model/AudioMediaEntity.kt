package com.kintmin.data.local_db.model

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * id?� source가 고유??Entity.
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
    val rawCreatedTime: Long = System.currentTimeMillis(),
)
