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
    /**
     * 사용자가 고른 썸네일 파일명. null이면 받아 온 정본([imageFileNameWithExt])을 쓴다.
     * 정본 파일은 바꾼 뒤에도 지우지 않아 언제든 되돌릴 수 있다.
     */
    val customImageFileNameWithExt: String? = null,
    val lyricFileNameWithExt: String? = null,
    /**
     * 편집본 시간축에 맞춰 다시 만든 가사 파일명. 원본 가사([lyricFileNameWithExt])에서 파생되며
     * 편집본이 있을 때만 채워진다. 원본 가사나 편집 배치가 바뀌면 다시 만든다.
     */
    val editedLyricFileNameWithExt: String? = null,
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

/**
 * 화면과 플레이리스트 커버에 쓸 썸네일 파일명. 고른 것이 있으면 그것, 없으면 정본.
 *
 * entity 바깥에 두는 이유는 Room 이 계산 프로퍼티를 컬럼으로 잡으려 들지 않게 하기 위해서다.
 */
internal val AudioMediaEntity.displayImageFileNameWithExt: String?
    get() = customImageFileNameWithExt ?: imageFileNameWithExt
