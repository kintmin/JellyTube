package com.kintmin.domain

import com.kintmin.domain.audio_edit.model.AudioEditPart
import com.kintmin.domain.audio_edit.model.AudioEditClip
import com.kintmin.domain.audio_edit.model.AudioEditTimeline
import com.kintmin.domain.audio_edit.model.AudioEditTimeline.Edge
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class AudioEditTimelineTest {

    private companion object {

        const val ORIGIN_DURATION_MS = 300_000L
    }

    private val initial = AudioEditTimeline.initial(ORIGIN_DURATION_MS)

    /** 옮기지 않은 조각은 타임라인 위치가 원본 시각과 같다. */
    private fun clip(startMs: Long, endMs: Long, positionMs: Long = startMs) =
        AudioEditClip(sourceStartMs = startMs, sourceEndMs = endMs, positionMs = positionMs)

    private fun audio(startMs: Long, endMs: Long) = AudioEditPart.Audio(startMs, endMs)

    private fun silence(durationMs: Long) = AudioEditPart.Silence(durationMs)

    @Test
    fun `처음에는 원본 전체가 조각 하나이고 끝 표시는 원본 끝이다`() {
        assertEquals(listOf(clip(0, ORIGIN_DURATION_MS)), initial.clips)
        assertEquals(ORIGIN_DURATION_MS, initial.endMs)
        assertFalse(initial.isDirty)
        assertEquals(listOf(audio(0, ORIGIN_DURATION_MS)), initial.toParts())
    }

    @Test
    fun `기준선에서 자르면 조각이 둘로 나뉜다`() {
        val result = initial.split(100_000)

        assertEquals(listOf(clip(0, 100_000), clip(100_000, ORIGIN_DURATION_MS)), result.clips)
        assertTrue(result.isDirty)
        // 나누기만 해서는 소리가 그대로 이어진다
        assertEquals(listOf(audio(0, 100_000), audio(100_000, ORIGIN_DURATION_MS)), result.toParts())
    }

    @Test
    fun `조각 끝에 너무 가까우면 자를 수 없다`() {
        assertFalse(initial.canSplit(50))
        assertFalse(initial.canSplit(ORIGIN_DURATION_MS - 50))
        assertEquals(initial, initial.split(50))
    }

    @Test
    fun `빈칸에서는 자를 수 없다`() {
        val timeline = initial.split(100_000).delete(0)

        assertFalse(timeline.canSplit(50_000))
    }

    @Test
    fun `조각을 지우면 그 자리는 무음으로 남는다`() {
        val result = initial.split(100_000).split(200_000).delete(1)

        assertEquals(
            listOf(audio(0, 100_000), silence(100_000), audio(200_000, ORIGIN_DURATION_MS)),
            result.toParts(),
        )
    }

    @Test
    fun `앞 조각을 지우면 결과 앞에 무음이 들어간다`() {
        val result = initial.split(10_000).delete(0)

        assertEquals(listOf(silence(10_000), audio(10_000, ORIGIN_DURATION_MS)), result.toParts())
    }

    @Test
    fun `빈칸 쪽으로 경계를 끌면 원본 소리가 되살아난다`() {
        val timeline = initial.split(100_000).split(200_000).delete(1)

        val result = timeline.resizeEdge(index = 0, edge = Edge.End, toMs = 150_000)

        assertEquals(clip(0, 150_000), result.clips[0])
    }

    @Test
    fun `경계를 안쪽으로 끌면 조각이 줄어든다`() {
        val result = initial.resizeEdge(index = 0, edge = Edge.Start, toMs = 20_000)

        assertEquals(listOf(clip(20_000, ORIGIN_DURATION_MS)), result.clips)
        assertEquals(listOf(silence(20_000), audio(20_000, ORIGIN_DURATION_MS)), result.toParts())
    }

    @Test
    fun `경계는 이웃 조각을 넘어 겹칠 수 없다`() {
        val timeline = initial.split(100_000).split(200_000).delete(1)

        val result = timeline.resizeEdge(index = 0, edge = Edge.End, toMs = 250_000)

        assertEquals(clip(0, 200_000), result.clips[0])
    }

    @Test
    fun `경계는 0초 이전이나 원본 끝 이후로 갈 수 없다`() {
        val shrunk = initial
            .resizeEdge(index = 0, edge = Edge.Start, toMs = 10_000)
            .resizeEdge(index = 0, edge = Edge.End, toMs = 290_000)

        val result = shrunk
            .resizeEdge(index = 0, edge = Edge.Start, toMs = -5_000)
            .resizeEdge(index = 0, edge = Edge.End, toMs = 400_000)

        assertEquals(listOf(clip(0, ORIGIN_DURATION_MS)), result.clips)
    }

    @Test
    fun `경계를 끌어도 최소 길이보다 짧아지지 않는다`() {
        val result = initial.resizeEdge(index = 0, edge = Edge.Start, toMs = ORIGIN_DURATION_MS)

        assertEquals(
            clip(ORIGIN_DURATION_MS - AudioEditTimeline.MIN_CLIP_MS, ORIGIN_DURATION_MS),
            result.clips[0],
        )
    }

    @Test
    fun `끝 표시를 늘리면 뒤에 무음이 붙는다`() {
        val result = initial.withEnd(320_000)

        assertEquals(listOf(audio(0, ORIGIN_DURATION_MS), silence(20_000)), result.toParts())
    }

    @Test
    fun `끝 표시를 앞당기면 그 뒤 소리가 잘린다`() {
        val result = initial.split(200_000).withEnd(150_000)

        assertEquals(listOf(audio(0, 150_000)), result.toParts())
    }

    @Test
    fun `끝 표시는 상한과 하한을 넘지 않는다`() {
        assertEquals(initial.maxEndMs, initial.withEnd(Long.MAX_VALUE).endMs)
        assertEquals(AudioEditTimeline.MIN_CLIP_MS, initial.withEnd(0).endMs)
    }

    @Test
    fun `끝 표시에 걸려 너무 짧아진 조각은 무음으로 바뀐다`() {
        val result = initial.split(100_000).withEnd(100_050)

        assertEquals(listOf(audio(0, 100_000), silence(50)), result.toParts())
    }

    @Test
    fun `조각을 모두 지우면 소리가 없다`() {
        val result = initial.delete(0)

        assertFalse(result.hasAudio)
        assertEquals(listOf(silence(ORIGIN_DURATION_MS)), result.toParts())
    }

    @Test
    fun `위치로 조각을 찾고 빈칸이면 null이다`() {
        val timeline = initial.split(100_000).split(200_000).delete(1)

        assertEquals(0, timeline.clipIndexAt(50_000))
        assertNull(timeline.clipIndexAt(150_000))
        assertEquals(1, timeline.clipIndexAt(250_000))
        assertNull(timeline.clipIndexAt(ORIGIN_DURATION_MS))
    }

    @Test
    fun `조각을 빈칸 안에서 옮기면 원본 구간은 그대로고 위치만 바뀐다`() {
        val timeline = initial.split(100_000).split(200_000).delete(1)

        val result = timeline.move(index = 0, toPositionMs = 50_000)

        assertEquals(clip(0, 100_000, positionMs = 50_000), result.clips[0])
        assertEquals(
            listOf(silence(50_000), audio(0, 100_000), silence(50_000), audio(200_000, ORIGIN_DURATION_MS)),
            result.toParts(),
        )
    }

    @Test
    fun `조각은 이웃 조각과 겹치도록 옮길 수 없다`() {
        val timeline = initial.split(100_000).split(200_000).delete(1)

        assertEquals(clip(0, 100_000, positionMs = 100_000), timeline.move(0, 150_000).clips[0])
        assertEquals(clip(0, 100_000, positionMs = 0), timeline.move(0, -10_000).clips[0])
    }

    @Test
    fun `조각을 끝 표시 너머로 옮기면 끝 표시가 따라 늘어나 앞에 무음이 생긴다`() {
        val result = initial.move(index = 0, toPositionMs = 10_000)

        assertEquals(310_000, result.endMs)
        assertEquals(listOf(silence(10_000), audio(0, ORIGIN_DURATION_MS)), result.toParts())
    }

    @Test
    fun `조각을 앞으로 옮겨도 끝 표시는 줄어들지 않는다`() {
        val result = initial.move(index = 0, toPositionMs = 10_000).move(index = 0, toPositionMs = 0)

        assertEquals(310_000, result.endMs)
        assertEquals(listOf(audio(0, ORIGIN_DURATION_MS), silence(10_000)), result.toParts())
    }

    @Test
    fun `경계를 끝 표시 너머로 늘리면 끝 표시가 따라 늘어난다`() {
        // 끝 표시를 200초로 당겨 뒤를 잘라낸 상태에서, 조각 끝도 150초로 줄였다가 다시 늘린다
        val timeline = initial.withEnd(200_000).resizeEdge(index = 0, edge = Edge.End, toMs = 150_000)

        val result = timeline.resizeEdge(index = 0, edge = Edge.End, toMs = 250_000)

        assertEquals(250_000, result.endMs)
    }

    @Test
    fun `옮긴 조각의 경계를 끌면 원본 범위 안에서만 되살아난다`() {
        // 원본 100~200초 조각을 타임라인 150초로 옮긴 상태
        val timeline = initial.split(100_000).split(200_000).delete(2).delete(0)
            .move(index = 0, toPositionMs = 150_000)

        val extended = timeline.resizeEdge(index = 0, edge = Edge.Start, toMs = 0)

        // 원본 0초까지만 되살아나므로 타임라인 50초에서 멈춘다
        assertEquals(clip(0, 200_000, positionMs = 50_000), extended.clips[0])
    }

    @Test
    fun `옮긴 조각에서 자르면 원본 시각 기준으로 나뉜다`() {
        val timeline = initial.split(100_000).split(200_000).delete(2).delete(0)
            .move(index = 0, toPositionMs = 150_000)

        val result = timeline.split(170_000)

        assertEquals(
            listOf(clip(100_000, 120_000, positionMs = 150_000), clip(120_000, 200_000, positionMs = 170_000)),
            result.clips,
        )
    }

    @Test
    fun `끝 표시로 잘라 둔 조각은 앞 경계를 건드려도 잘린 길이가 유지된다`() {
        val timeline = initial.withEnd(150_000)

        val result = timeline.resizeEdge(index = 0, edge = Edge.Start, toMs = 20_000)

        assertEquals(150_000, result.endMs)
    }

    @Test
    fun `끝 표시로 잘라 둔 조각을 뒤로 옮기면 옮긴 만큼만 끝 표시가 따라간다`() {
        val timeline = initial.withEnd(150_000).resizeEdge(index = 0, edge = Edge.Start, toMs = 20_000)

        val result = timeline.move(index = 0, toPositionMs = 30_000)

        assertEquals(160_000, result.endMs)
    }
}
