package com.kintmin.domain.audio_edit.model

/**
 * 편집 결과를 이루는 조각. 순서대로 이어붙인 것이 편집본이다.
 *
 * 렌더와 미리듣기가 같은 목록을 받아서, 들어본 것과 저장된 것이 어긋나지 않는다.
 */
sealed interface AudioEditPart {

    val durationMs: Long

    /** 원본의 [startMs]~[endMs] 구간을 그대로 재생한다. */
    data class Audio(
        val startMs: Long,
        val endMs: Long,
    ) : AudioEditPart {

        override val durationMs: Long get() = endMs - startMs
    }

    /** 조각을 지운 빈칸이나 끝 표시를 늘려 생긴 공백. 무음으로 채운다. */
    data class Silence(
        override val durationMs: Long,
    ) : AudioEditPart
}
