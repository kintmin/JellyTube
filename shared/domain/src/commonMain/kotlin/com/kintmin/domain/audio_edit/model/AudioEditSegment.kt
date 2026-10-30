package com.kintmin.domain.audio_edit.model

/**
 * 편집 결과에 남길 구간. 원본 음원 기준 밀리초 오프셋이다.
 *
 * 앞뒤만 자르는 편집은 구간 1개, 중간을 잘라내는 편집은 구간 2개 이상이 되고,
 * 구간들을 순서대로 이어붙인 것이 편집본이다.
 */
data class AudioEditSegment(
    val startMs: Long,
    val endMs: Long,
) {

    val durationMs: Long get() = endMs - startMs
}
