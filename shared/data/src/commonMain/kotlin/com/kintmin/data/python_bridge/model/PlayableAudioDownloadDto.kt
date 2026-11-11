package com.kintmin.data.python_bridge.model

data class PlayableAudioDownloadDto(
    val audioFileNameWithExt: String,
    /** 초. 모르면 빈 문자열. */
    val duration: String,
)
