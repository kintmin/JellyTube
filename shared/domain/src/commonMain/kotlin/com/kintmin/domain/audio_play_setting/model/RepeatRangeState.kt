package com.kintmin.domain.audio_play_setting.model

import kotlin.time.Duration

data class RepeatRangeState(
    val mediaId: String? = null,
    val startDuration: Duration? = null,
    val endDuration: Duration? = null,
)
