package com.kintmin.data.python_bridge

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

class RangeChunkPlanTest {

    /**
     * 이 상한을 넘기면 CDN 이 통짜 GET 과 같은 속도로 조인다. 실측으로 10MiB 와 12MB 사이에
     * 절벽이 있어 상한은 정확히 10MiB 다.
     */
    @Test
    fun `요청 범위는 10MiB 를 넘지 않는다`() {
        assertEquals(10L * 1024 * 1024, RangeChunkPlan.MAX_CHUNK_BYTES)

        val spans = buildList {
            add(RangeChunkPlan.first())
            listOf(1L, 10L * 1024 * 1024, 58_263_890L, Long.MAX_VALUE / 2).forEach { total ->
                var received = 0L
                while (true) {
                    val chunk = RangeChunkPlan.next(received, total) ?: break
                    add(chunk)
                    received = chunk.endInclusive + 1L
                    if (size > 64) break
                }
            }
        }

        assertTrue(spans.isNotEmpty())
        spans.forEach { chunk ->
            val span = chunk.endInclusive - chunk.start + 1L
            assertTrue(span in 1L..RangeChunkPlan.MAX_CHUNK_BYTES, "범위 폭 $span")
        }
    }

    @Test
    fun `첫 요청은 전체 크기를 몰라도 상한만큼 달라고 한다`() {
        assertEquals(RangeChunk(0L, 10_485_759L), RangeChunkPlan.first())
        assertEquals("bytes=0-10485759", RangeChunkPlan.first().headerValue)
    }

    @Test
    fun `상한 이하 파일은 한 번에 받는다`() {
        assertEquals(RangeChunk(0L, 378_742L), RangeChunkPlan.next(0L, 378_743L))
        assertNull(RangeChunkPlan.next(378_743L, 378_743L))
    }

    @Test
    fun `이어받을 때 받은 다음 바이트부터 요청한다`() {
        assertEquals(RangeChunk(10_485_760L, 20_971_519L), RangeChunkPlan.next(10_485_760L, 58_263_890L))
    }

    @Test
    fun `마지막 청크는 전체 크기에서 끊는다`() {
        assertEquals(RangeChunk(52_428_800L, 58_263_889L), RangeChunkPlan.next(52_428_800L, 58_263_890L))
    }

    @Test
    fun `다 받았으면 더 요청하지 않는다`() {
        assertNull(RangeChunkPlan.next(58_263_890L, 58_263_890L))
        assertNull(RangeChunkPlan.next(58_263_891L, 58_263_890L))
    }

    @Test
    fun `청크를 이어 붙이면 빈틈도 겹침도 없다`() {
        val total = 58_263_890L
        var received = 0L
        var requestCount = 0
        while (true) {
            val chunk = RangeChunkPlan.next(received, total) ?: break
            assertEquals(received, chunk.start)
            received = chunk.endInclusive + 1L
            requestCount++
        }
        assertEquals(total, received)
        assertEquals(6, requestCount)
    }

    /**
     * 하나씩 이어서 등록하면 그때마다 OS 가 앱을 깨워야 하고, 서스펜드 상태에서 그 깨우기가
     * 청크당 10초를 넘는다. 48.8MB 를 포그라운드 6.4초 · 백그라운드 73.8초로 실측했다.
     */
    @Test
    fun `전체 목록은 빈틈도 겹침도 없이 파일을 덮는다`() {
        listOf(1L, 378_743L, 10_485_760L, 10_485_761L, 51_176_868L, 58_263_890L).forEach { total ->
            val chunks = RangeChunkPlan.chunks(total)

            assertEquals(0L, chunks.first().start, "total=$total")
            assertEquals(total - 1L, chunks.last().endInclusive, "total=$total")
            chunks.zipWithNext { current, nextChunk ->
                assertEquals(current.endInclusive + 1L, nextChunk.start, "total=$total")
            }
            chunks.forEach { chunk ->
                val span = chunk.endInclusive - chunk.start + 1L
                assertTrue(span in 1L..RangeChunkPlan.MAX_CHUNK_BYTES, "total=$total 범위 폭 $span")
            }
        }
    }

    @Test
    fun `전체 목록의 개수는 상한으로 나눈 올림이다`() {
        assertEquals(1, RangeChunkPlan.chunks(1L).size)
        assertEquals(1, RangeChunkPlan.chunks(10_485_760L).size)
        assertEquals(2, RangeChunkPlan.chunks(10_485_761L).size)
        assertEquals(5, RangeChunkPlan.chunks(51_176_868L).size)
        assertTrue(RangeChunkPlan.chunks(0L).isEmpty())
        assertTrue(RangeChunkPlan.chunks(-1L).isEmpty())
    }

    @Test
    fun `보낸 Range 헤더에서 어느 구간인지 되읽는다`() {
        val chunk = RangeChunkPlan.chunks(51_176_868L)[3]
        assertEquals(chunk.start, RangeChunkPlan.parseRequestedStart(chunk.headerValue))
        assertEquals(0L, RangeChunkPlan.parseRequestedStart("bytes=0-10485759"))
        assertNull(RangeChunkPlan.parseRequestedStart("bytes=-500"))
        assertNull(RangeChunkPlan.parseRequestedStart("10485760-20971519"))
        assertNull(RangeChunkPlan.parseRequestedStart(null))
    }

    @Test
    fun `Content-Range 에서 전체 크기를 읽는다`() {
        assertEquals(58_263_890L, RangeChunkPlan.parseTotalBytes("bytes 0-10485759/58263890"))
        assertNull(RangeChunkPlan.parseTotalBytes("bytes 0-10485759/*"))
        assertNull(RangeChunkPlan.parseTotalBytes("bytes */0"))
        assertNull(RangeChunkPlan.parseTotalBytes(""))
        assertNull(RangeChunkPlan.parseTotalBytes(null))
    }
}
