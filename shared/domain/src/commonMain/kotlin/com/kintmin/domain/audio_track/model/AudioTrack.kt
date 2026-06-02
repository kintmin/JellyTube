package com.kintmin.domain.audio_track.model

import kotlinx.datetime.LocalDateTime

data class AudioTrack(
    val audioMediaId: Int,
    val playlistId: Int,
    val trackSequence: Int,
    val trackAddedTime: LocalDateTime,
)
