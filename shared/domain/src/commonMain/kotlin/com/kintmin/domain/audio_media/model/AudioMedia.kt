package com.kintmin.domain.audio_media.model

import kotlinx.datetime.LocalDateTime
import kotlin.time.Duration
import kotlin.time.Duration.Companion.seconds

data class AudioMedia(
    val id: Int,
    val source: String,
    val name: String,
    val artist: String,
    val description: String,
    /** 원본 길이. 편집본이 있어도 바뀌지 않는다(가사 검색의 길이 매칭이 이 값을 쓴다). */
    val audioDuration: Duration?,
    /** 원본 파일 경로. 편집본이 있어도 바뀌지 않는다. */
    val audioFileFullPath: String,
    val imageFileFullPath: String?,
    val lyricFileFullPath: String? = null,
    val tjKaraokeNumber: String? = null,
    /** 편집본 경로. null이면 편집본이 없다. */
    val editedAudioFileFullPath: String? = null,
    /** 편집본 길이. [editedAudioFileFullPath]와 항상 같이 채워진다. */
    val editedAudioDuration: Duration? = null,
    val createdTime: LocalDateTime,
) {

    /** 재생·내보내기에 쓸 경로. 편집본이 있으면 편집본, 없으면 원본. */
    val playbackFileFullPath: String get() = editedAudioFileFullPath ?: audioFileFullPath

    /** 재생·화면 표시에 쓸 길이. 편집본이 있으면 편집본, 없으면 원본. */
    val playbackDuration: Duration? get() = editedAudioDuration ?: audioDuration

    /** 길이를 모르면(null) 막지 않는다 — 길면 시스템이 등록을 거부한다. */
    val fitsRingtoneLength: Boolean get() = playbackDuration?.let { it < RINGTONE_MAX_DURATION } ?: true

    companion object {

        /** iOS 가 벨소리로 받는 길이의 상한. 이 값 **미만**만 등록된다. */
        val RINGTONE_MAX_DURATION: Duration = 30.seconds
    }
}
