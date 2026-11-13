package com.kintmin.data.audio_edit

/** 음원 [totalFrameCount] 프레임을 파형 [bucketCount] 칸으로 고르게 나눈다. */
internal class WaveformBuckets(
    private val totalFrameCount: Long,
    private val bucketCount: Int,
) {

    init {
        require(totalFrameCount > 0 && bucketCount > 0)
    }

    fun indexOf(frame: Long): Int = (frame * bucketCount / totalFrameCount).toInt().coerceIn(0, bucketCount - 1)

    /** [index] 칸 다음 칸이 시작하는 프레임. */
    fun endFrameOf(index: Int): Long = ((index + 1L) * totalFrameCount + bucketCount - 1) / bucketCount

    fun loadedCountAfter(readFrameCount: Long): Int = (readFrameCount * bucketCount / totalFrameCount).toInt()
}
