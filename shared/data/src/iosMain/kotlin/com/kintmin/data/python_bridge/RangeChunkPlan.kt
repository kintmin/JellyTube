package com.kintmin.data.python_bridge

/** 한 번에 요청할 바이트 범위. 그대로 `Range` 헤더가 된다. */
data class RangeChunk(
    val start: Long,
    val endInclusive: Long,
) {
    val headerValue: String get() = "bytes=$start-$endInclusive"
}

/**
 * 유튜브 CDN 전송을 범위 요청으로 쪼갠다.
 *
 * 한 요청이 [MAX_CHUNK_BYTES] 를 넘는 범위를 달라고 하면 CDN 이 통짜 GET 과 같은 속도로 조인다
 * (실측: 10MiB 9,429KB/s · 12MB 31KB/s · 범위 없음 30KB/s). 범위를 파일 전체로 잡는 것도 같다.
 */
object RangeChunkPlan {

    const val MAX_CHUNK_BYTES: Long = 10L * 1024 * 1024

    /** 전체 크기를 알기 전의 첫 요청. 크기는 이 응답의 `Content-Range` 로 알게 된다. */
    fun first(): RangeChunk = RangeChunk(start = 0L, endInclusive = MAX_CHUNK_BYTES - 1L)

    /** [receivedBytes] 까지 받았을 때 다음에 요청할 범위. 다 받았으면 null. */
    fun next(receivedBytes: Long, totalBytes: Long): RangeChunk? {
        val start = receivedBytes.coerceAtLeast(0L)
        if (start >= totalBytes) return null

        return RangeChunk(
            start = start,
            endInclusive = minOf(start + MAX_CHUNK_BYTES - 1L, totalBytes - 1L),
        )
    }

    /**
     * [totalBytes] 를 받으려면 요청해야 하는 범위 전부.
     *
     * 앱이 떠 있는 동안 이것들을 한꺼번에 등록해야 한다. 하나씩 이어서 등록하면 그때마다 OS 가
     * 앱을 깨워야 하고, 서스펜드 상태에서 그 깨우기가 청크당 10초를 넘는다.
     */
    fun chunks(totalBytes: Long): List<RangeChunk> {
        if (totalBytes <= 0L) return emptyList()

        return buildList {
            var start = 0L
            while (start < totalBytes) {
                val chunk = next(start, totalBytes) ?: break
                add(chunk)
                start = chunk.endInclusive + 1L
            }
        }
    }

    /** `bytes=10485760-20971519` 에서 시작 오프셋. 어느 구간의 응답인지 가리는 데 쓴다. */
    fun parseRequestedStart(rangeHeader: String?): Long? =
        rangeHeader?.substringAfter('=', "")?.substringBefore('-')?.trim()?.toLongOrNull()
            ?.takeIf { it >= 0L }

    /** `bytes 0-10485759/58263890` 에서 전체 크기. 크기를 모르는 `*` 응답이면 null. */
    fun parseTotalBytes(contentRange: String?): Long? =
        contentRange?.substringAfterLast('/')?.trim()?.toLongOrNull()?.takeIf { it > 0L }
}
