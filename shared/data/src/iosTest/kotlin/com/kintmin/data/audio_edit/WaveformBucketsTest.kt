package com.kintmin.data.audio_edit

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class WaveformBucketsTest {

    @Test
    fun `칸은 빈틈 없이 이어지고 마지막 칸이 음원 끝에서 끝난다`() {
        listOf(1_000L to 7, 10L to 3, 3L to 10, 441_000L to 400).forEach { (totalFrameCount, bucketCount) ->
            val buckets = WaveformBuckets(totalFrameCount, bucketCount)

            var frame = 0L
            while (frame < totalFrameCount) {
                val index = buckets.indexOf(frame)
                val endFrame = buckets.endFrameOf(index)
                assertTrue(endFrame > frame, "frame=$frame index=$index end=$endFrame")
                // 칸 안의 프레임은 모두 같은 칸이고, 끝 프레임부터는 다음 칸이다
                assertEquals(index, buckets.indexOf(endFrame - 1))
                if (endFrame < totalFrameCount) assertTrue(buckets.indexOf(endFrame) > index)
                frame = endFrame
            }
            assertEquals(totalFrameCount, buckets.endFrameOf(buckets.indexOf(totalFrameCount - 1)))
        }
    }

    @Test
    fun `읽던 칸은 다 채워진 칸으로 세지 않는다`() {
        val buckets = WaveformBuckets(totalFrameCount = 1_000, bucketCount = 10)

        assertEquals(0, buckets.loadedCountAfter(99))
        assertEquals(1, buckets.loadedCountAfter(100))
        assertEquals(10, buckets.loadedCountAfter(1_000))
    }
}
