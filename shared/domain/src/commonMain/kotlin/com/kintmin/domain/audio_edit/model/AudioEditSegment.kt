package com.kintmin.domain.audio_edit.model

/**
 * 원본 음원 기준 밀리초 시간 구간. 남길 구간과 잘라낼 구간 모두 이 타입으로 표현한다.
 *
 * 편집은 "잘라낼 구간"을 모아 두고 그 여집합을 남기는 방식이다(BuildAudioEditSegmentsUseCase).
 * 앞뒤만 자르면 남는 구간이 1개, 중간을 잘라내면 2개 이상이 되고,
 * 남은 구간들을 순서대로 이어붙인 것이 편집본이다.
 */
data class AudioEditSegment(
    val startMs: Long,
    val endMs: Long,
) {

    val durationMs: Long get() = endMs - startMs
}
