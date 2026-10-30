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
    /**
     * 바로 앞 조각과 병합되어 한 덩이로 움직이는지. 이어진 조각들이 하나의 묶음이 된다.
     *
     * 원본에서 이어진 두 조각은 병합하면 아예 한 조각으로 합치므로, 이 값이 true인 조각은
     * 앞 조각과 원본 구간이 이어지지 않는다. 저장·미리듣기는 조각을 순서대로 이을 뿐이라 이 값을 보지 않는다.
     */
    val isLinkedToPrevious: Boolean = false,
) {

    val durationMs: Long get() = sourceEndMs - sourceStartMs

    /** 타임라인에서 이 조각이 끝나는 위치. */
    val positionEndMs: Long get() = positionMs + durationMs

    /** 타임라인 위치 [ms]에 해당하는 원본 시각. */
    fun sourceMsAt(ms: Long): Long = sourceStartMs + (ms - positionMs)
}
