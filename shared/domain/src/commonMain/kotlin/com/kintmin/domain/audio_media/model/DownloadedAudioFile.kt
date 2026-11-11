package com.kintmin.domain.audio_media.model

/** 행 없이 음원 파일만 받은 결과. 썸네일·메타데이터는 받지 않는다. */
data class DownloadedAudioFile(
    val fileNameWithExt: String,
    val fileFullPath: String,
    val durationSeconds: Long?,
)
