package com.kintmin.domain.audio_edit.model

/**
 * 파형 표시용 진폭. 원본을 [peaks] 크기만큼 고르게 나눈 구간별 최대 진폭이다.
 *
 * 긴 음원은 끝까지 읽는 데 오래 걸려 앞에서부터 채워지며 여러 번 흘러온다.
 * [loadedBucketCount]까지만 실제 값이고, 그 뒤는 아직 읽지 않아 0이다.
 *
 * 값은 16비트 기준 절대 진폭(0~[FULL_SCALE_PEAK])이다. 그릴 때 [FULL_SCALE_PEAK]를 최대 높이로 고정해,
 * 곡마다나 읽는 도중에 파형 높이가 달라지지 않게 한다(일반 오디오 편집기의 0 dBFS 기준과 같다).
 */
data class AudioWaveform(
    val peaks: List<Int>,
    val loadedBucketCount: Int,
) {

    val isComplete: Boolean get() = loadedBucketCount >= peaks.size

    companion object {

        /** 디지털 최대 음량(0 dBFS)에 해당하는 진폭. */
        const val FULL_SCALE_PEAK = 32_767

        val EMPTY = AudioWaveform(peaks = emptyList(), loadedBucketCount = 0)
    }
}
