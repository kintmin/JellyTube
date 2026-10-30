package com.kintmin.domain.audio_edit.usecase

import com.kintmin.domain.audio_edit.model.AudioEditSegment

/**
 * 잘라낼 구간 목록에서 "남길 구간" 목록을 만든다.
 *
 * 편집 화면은 사용자가 고른 잘라낼 구간만 들고 있고, 실제 렌더에 넘길 남길 구간은 이 계산의 결과다.
 * 앞을 자르면 뒤 한 덩어리, 중간을 자르면 두 덩어리가 남는 식이라 트림과 중간 삭제를 한 가지 조작으로 다룰 수 있다.
 *
 * 입력은 겹치거나 순서가 뒤섞여 있어도 되고, 음원 범위를 벗어난 값도 받는다.
 */
class BuildAudioEditSegmentsUseCase constructor() {

    private companion object {

        /** 이보다 짧게 남는 조각은 재생 의미가 없어 버린다. */
        const val MIN_SEGMENT_DURATION_MS = 100L
    }

    operator fun invoke(
        originDurationMs: Long,
        cutRanges: List<AudioEditSegment>,
    ): List<AudioEditSegment> {
        if (originDurationMs <= 0) return emptyList()

        val mergedCuts = cutRanges
            // 음원 범위로 자르고 뒤집힌 구간은 버린다
            .map {
                AudioEditSegment(
                    startMs = it.startMs.coerceIn(0, originDurationMs),
                    endMs = it.endMs.coerceIn(0, originDurationMs),
                )
            }
            .filter { it.durationMs > 0 }
            .sortedBy { it.startMs }
            .fold(mutableListOf<AudioEditSegment>()) { merged, cut ->
                val last = merged.lastOrNull()
                if (last != null && cut.startMs <= last.endMs) {
                    // 겹치거나 맞닿은 구간은 하나로 합친다
                    merged[merged.lastIndex] = last.copy(endMs = maxOf(last.endMs, cut.endMs))
                } else {
                    merged += cut
                }
                merged
            }

        // 잘라낸 구간의 여집합이 남길 구간이다
        val segments = mutableListOf<AudioEditSegment>()
        var cursorMs = 0L
        mergedCuts.forEach { cut ->
            if (cut.startMs > cursorMs) {
                segments += AudioEditSegment(startMs = cursorMs, endMs = cut.startMs)
            }
            cursorMs = maxOf(cursorMs, cut.endMs)
        }
        if (cursorMs < originDurationMs) {
            segments += AudioEditSegment(startMs = cursorMs, endMs = originDurationMs)
        }

        return segments.filter { it.durationMs >= MIN_SEGMENT_DURATION_MS }
    }
}
