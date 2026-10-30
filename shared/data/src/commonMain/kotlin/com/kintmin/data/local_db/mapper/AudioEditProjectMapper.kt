package com.kintmin.data.local_db.mapper

import com.kintmin.domain.audio_edit.model.AudioEditClip
import com.kintmin.domain.audio_edit.model.AudioEditTimeline
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

/**
 * AUDIO_EDIT_PROJECT.timelineJson 형식.
 *
 * 도메인 모델은 아무것도 의존하지 않아야 해서 직렬화 표시는 이 전용 DTO에만 둔다.
 * [version]은 타임라인 구조가 바뀌어도 DB 마이그레이션 없이 읽는 쪽에서 처리하기 위한 값이다.
 */
@Serializable
private data class AudioEditTimelineJson(
    val version: Int = CURRENT_VERSION,
    val originDurationMs: Long,
    val endMs: Long,
    val clips: List<AudioEditClipJson>,
)

@Serializable
private data class AudioEditClipJson(
    val sourceStartMs: Long,
    val sourceEndMs: Long,
    val positionMs: Long,
    val isLinkedToPrevious: Boolean = false,
)

private const val CURRENT_VERSION = 1

/** 모르는 필드가 생겨도 읽을 수 있게 한다. 앱을 되돌려 설치한 경우 등. */
private val timelineJsonFormat = Json { ignoreUnknownKeys = true }

internal fun AudioEditTimeline.toTimelineJson(): String = timelineJsonFormat.encodeToString(
    AudioEditTimelineJson.serializer(),
    AudioEditTimelineJson(
        originDurationMs = originDurationMs,
        endMs = endMs,
        clips = clips.map { clip ->
            AudioEditClipJson(
                sourceStartMs = clip.sourceStartMs,
                sourceEndMs = clip.sourceEndMs,
                positionMs = clip.positionMs,
                isLinkedToPrevious = clip.isLinkedToPrevious,
            )
        },
    ),
)

/** 읽을 수 없는 JSON이면 null. 편집 상태를 잃을 뿐 편집본 자체는 그대로라 원본에서 다시 시작하면 된다. */
internal fun String.toAudioEditTimelineOrNull(): AudioEditTimeline? = runCatching {
    val json = timelineJsonFormat.decodeFromString(AudioEditTimelineJson.serializer(), this)
    AudioEditTimeline(
        originDurationMs = json.originDurationMs,
        clips = json.clips.map { clip ->
            AudioEditClip(
                sourceStartMs = clip.sourceStartMs,
                sourceEndMs = clip.sourceEndMs,
                positionMs = clip.positionMs,
                isLinkedToPrevious = clip.isLinkedToPrevious,
            )
        },
        endMs = json.endMs,
    )
}.getOrNull()
