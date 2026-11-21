package com.kintmin.domain.audio_track.model

import kotlin.time.Duration

/** 비밀 플레이리스트에 하나라도 속한 음원. */
data class SecretTrack(
    val audioMediaId: Int,
    val playbackDuration: Duration,
)
