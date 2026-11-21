package com.kintmin.domain.audio_media

/** 받기 전에 데이터 사용량을 미리 보여 주는 어림값이다. */
object DownloadSizeEstimate {

    // 받는 형식이 대개 128kbps AAC 다.
    private const val BYTES_PER_SECOND = 16_000L
    private const val FALLBACK_DURATION_SECONDS = 240L

    fun bytesFor(durationSeconds: Long?): Long = (durationSeconds ?: FALLBACK_DURATION_SECONDS) * BYTES_PER_SECOND
}
