package com.kintmin.domain.audio_edit.model

/**
 * 편집 타임라인의 조각 하나. 원본의 [sourceStartMs]~[sourceEndMs] 소리를 타임라인 [positionMs]에 놓는다.
 *
 * 조각을 옮기면 [positionMs]만 바뀌고, 경계를 끌면 원본 구간과 위치가 함께 바뀐다.
 */
data class AudioEditClip(
    val sourceStartMs: Long,
    val sourceEndMs: Long,
    val positionMs: Long,
) {

    val durationMs: Long get() = sourceEndMs - sourceStartMs

    /** 타임라인에서 이 조각이 끝나는 위치. */
    val positionEndMs: Long get() = positionMs + durationMs

    /** 타임라인 위치 [ms]에 해당하는 원본 시각. */
    fun sourceMsAt(ms: Long): Long = sourceStartMs + (ms - positionMs)
}
