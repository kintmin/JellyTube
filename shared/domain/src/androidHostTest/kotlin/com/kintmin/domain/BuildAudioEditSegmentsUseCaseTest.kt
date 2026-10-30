package com.kintmin.domain

import com.kintmin.domain.audio_edit.model.AudioEditSegment
import com.kintmin.domain.audio_edit.usecase.BuildAudioEditSegmentsUseCase
import org.junit.Assert.assertEquals
import org.junit.Test

class BuildAudioEditSegmentsUseCaseTest {

    private val buildAudioEditSegmentsUseCase = BuildAudioEditSegmentsUseCase()

    private companion object {

        const val ORIGIN_DURATION_MS = 300_000L
    }

    private fun segment(startMs: Long, endMs: Long) = AudioEditSegment(startMs, endMs)

    @Test
    fun `잘라낸 구간이 없으면 전체가 남는다`() {
        val result = buildAudioEditSegmentsUseCase(ORIGIN_DURATION_MS, emptyList())

        assertEquals(listOf(segment(0, ORIGIN_DURATION_MS)), result)
    }

    @Test
    fun `앞을 자르면 뒤 한 덩어리가 남는다`() {
        val result = buildAudioEditSegmentsUseCase(
            originDurationMs = ORIGIN_DURATION_MS,
            cutRanges = listOf(segment(0, 10_000)),
        )

        assertEquals(listOf(segment(10_000, ORIGIN_DURATION_MS)), result)
    }

    @Test
    fun `뒤를 자르면 앞 한 덩어리가 남는다`() {
        val result = buildAudioEditSegmentsUseCase(
            originDurationMs = ORIGIN_DURATION_MS,
            cutRanges = listOf(segment(290_000, ORIGIN_DURATION_MS)),
        )

        assertEquals(listOf(segment(0, 290_000)), result)
    }

    @Test
    fun `중간을 자르면 두 덩어리가 남는다`() {
        val result = buildAudioEditSegmentsUseCase(
            originDurationMs = ORIGIN_DURATION_MS,
            cutRanges = listOf(segment(100_000, 150_000)),
        )

        assertEquals(
            listOf(segment(0, 100_000), segment(150_000, ORIGIN_DURATION_MS)),
            result,
        )
    }

    @Test
    fun `앞뒤와 중간을 모두 자르면 가운데 덩어리들만 남는다`() {
        val result = buildAudioEditSegmentsUseCase(
            originDurationMs = ORIGIN_DURATION_MS,
            cutRanges = listOf(
                segment(0, 10_000),
                segment(100_000, 150_000),
                segment(290_000, ORIGIN_DURATION_MS),
            ),
        )

        assertEquals(
            listOf(segment(10_000, 100_000), segment(150_000, 290_000)),
            result,
        )
    }

    @Test
    fun `순서가 뒤섞여 들어와도 시간순으로 계산한다`() {
        val result = buildAudioEditSegmentsUseCase(
            originDurationMs = ORIGIN_DURATION_MS,
            cutRanges = listOf(
                segment(290_000, ORIGIN_DURATION_MS),
                segment(0, 10_000),
            ),
        )

        assertEquals(listOf(segment(10_000, 290_000)), result)
    }

    @Test
    fun `겹치는 구간은 하나로 합쳐진다`() {
        val result = buildAudioEditSegmentsUseCase(
            originDurationMs = ORIGIN_DURATION_MS,
            cutRanges = listOf(
                segment(100_000, 150_000),
                segment(120_000, 200_000),
            ),
        )

        assertEquals(
            listOf(segment(0, 100_000), segment(200_000, ORIGIN_DURATION_MS)),
            result,
        )
    }

    @Test
    fun `맞닿은 구간도 하나로 합쳐진다`() {
        val result = buildAudioEditSegmentsUseCase(
            originDurationMs = ORIGIN_DURATION_MS,
            cutRanges = listOf(
                segment(100_000, 150_000),
                segment(150_000, 200_000),
            ),
        )

        assertEquals(
            listOf(segment(0, 100_000), segment(200_000, ORIGIN_DURATION_MS)),
            result,
        )
    }

    @Test
    fun `음원 범위를 벗어난 구간은 범위 안으로 잘린다`() {
        val result = buildAudioEditSegmentsUseCase(
            originDurationMs = ORIGIN_DURATION_MS,
            cutRanges = listOf(segment(-5_000, 10_000), segment(290_000, 999_999)),
        )

        assertEquals(listOf(segment(10_000, 290_000)), result)
    }

    @Test
    fun `뒤집힌 구간과 길이 0 구간은 무시한다`() {
        val result = buildAudioEditSegmentsUseCase(
            originDurationMs = ORIGIN_DURATION_MS,
            cutRanges = listOf(segment(150_000, 100_000), segment(50_000, 50_000)),
        )

        assertEquals(listOf(segment(0, ORIGIN_DURATION_MS)), result)
    }

    @Test
    fun `전체를 자르면 남는 구간이 없다`() {
        val result = buildAudioEditSegmentsUseCase(
            originDurationMs = ORIGIN_DURATION_MS,
            cutRanges = listOf(segment(0, ORIGIN_DURATION_MS)),
        )

        assertEquals(emptyList<AudioEditSegment>(), result)
    }

    @Test
    fun `너무 짧게 남는 조각은 버린다`() {
        val result = buildAudioEditSegmentsUseCase(
            originDurationMs = ORIGIN_DURATION_MS,
            // 0~50ms 가 남지만 재생 의미가 없다
            cutRanges = listOf(segment(50, ORIGIN_DURATION_MS)),
        )

        assertEquals(emptyList<AudioEditSegment>(), result)
    }

    @Test
    fun `길이를 알 수 없는 음원은 빈 목록을 돌려준다`() {
        val result = buildAudioEditSegmentsUseCase(
            originDurationMs = 0,
            cutRanges = listOf(segment(0, 1_000)),
        )

        assertEquals(emptyList<AudioEditSegment>(), result)
    }
}
