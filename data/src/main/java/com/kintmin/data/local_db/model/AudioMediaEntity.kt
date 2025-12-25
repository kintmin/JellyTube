package com.kintmin.data.local_db.model

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.time.Instant

/**
 * id와 source가 고유한 Entity.
 * 조인 최적화를 고려하여 PK는 Int로 설정.
 * 미디어가 21억개 이상이 될 일이 없기 때문에 id는 Long이 아닌 Int로 제한.
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
    val rawCreatedTime: Long = Instant.now().toEpochMilli(),
)
