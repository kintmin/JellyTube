package com.kintmin.domain

import com.kintmin.domain.audio_edit.model.AudioEditFrameSpan
import com.kintmin.domain.audio_edit.model.AudioEditPart
import com.kintmin.domain.audio_edit.model.toFrameSpans
import org.junit.Assert.assertEquals
import org.junit.Test

class AudioEditFrameSpanTest {

    private companion object {

        /** 1kHz 로 두면 1ms 가 1프레임이라 기댓값을 바로 읽을 수 있다. */
        const val SAMPLE_RATE = 1_000.0
        const val SOURCE_FRAME_COUNT = 10_000L
    }

    private fun audio(startMs: Long, endMs: Long) = AudioEditPart.Audio(startMs, endMs)

    private fun silence(durationMs: Long) = AudioEditPart.Silence(durationMs)

    private fun List<AudioEditPart>.spans(startAtMs: Long = 0L) =
        toFrameSpans(sampleRate = SAMPLE_RATE, sourceFrameCount = SOURCE_FRAME_COUNT, startAtMs = startAtMs)

    @Test
    fun `조각을 순서대로 프레임으로 바꾼다`() {
        val spans = listOf(silence(1_000), audio(2_000, 3_000), audio(500, 1_000)).spans()

        assertEquals(
            listOf(
                AudioEditFrameSpan.Silence(frameCount = 1_000),
                AudioEditFrameSpan.Audio(sourceStartFrame = 2_000, frameCount = 1_000),
                AudioEditFrameSpan.Audio(sourceStartFrame = 500, frameCount = 500),
            ),
            spans,
        )
    }

    @Test
    fun `조각마다 반올림해도 전체 프레임 수는 전체 길이에서 반올림한 값과 같다`() {
        // 44.1kHz 에서 1ms 는 44.1 프레임이라 조각마다 반올림하면 0.1 프레임씩 모자란다
        val parts = List(100) { audio(it.toLong(), it + 1L) }

        val spans = parts.toFrameSpans(sampleRate = 44_100.0, sourceFrameCount = 44_100L)

        assertEquals(4_410L, spans.sumOf { it.frameCount })
    }

    @Test
    fun `시작 시각이 조각 중간이면 그 조각을 잘라 시작한다`() {
        val spans = listOf(silence(1_000), audio(5_000, 7_000)).spans(startAtMs = 1_500)

        assertEquals(listOf(AudioEditFrameSpan.Audio(sourceStartFrame = 5_500, frameCount = 1_500)), spans)
    }

    @Test
    fun `시작 시각이 무음 중간이면 남은 무음부터 시작한다`() {
        val spans = listOf(audio(0, 1_000), silence(1_000)).spans(startAtMs = 1_200)

        assertEquals(listOf(AudioEditFrameSpan.Silence(frameCount = 800)), spans)
    }

    @Test
    fun `시작 시각이 끝 뒤면 빈 목록이다`() {
        assertEquals(emptyList<AudioEditFrameSpan>(), listOf(audio(0, 1_000)).spans(startAtMs = 2_000))
    }

    @Test
    fun `원본 끝을 넘는 몫은 무음으로 채워 뒤 조각이 당겨지지 않는다`() {
        val spans = listOf(audio(9_500, 10_500), audio(0, 100)).spans()

        assertEquals(
            listOf(
                AudioEditFrameSpan.Audio(sourceStartFrame = 9_500, frameCount = 500),
                AudioEditFrameSpan.Silence(frameCount = 500),
                AudioEditFrameSpan.Audio(sourceStartFrame = 0, frameCount = 100),
            ),
            spans,
        )
    }

    @Test
    fun `원본 끝 뒤에서 시작하는 조각은 통째로 무음이다`() {
        assertEquals(listOf(AudioEditFrameSpan.Silence(frameCount = 500)), listOf(audio(10_200, 10_700)).spans())
    }
}
