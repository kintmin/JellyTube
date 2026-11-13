package com.kintmin.domain.audio_media.model

/** 유튜브에서 받은 음원 파일과 그 정보. 썸네일은 아직 받지 않았다. */
data class DownloadedYoutubeAudio(
    /** 확장자 없는 저장 이름. 썸네일도 이 이름으로 저장한다. */
    val fileName: String,
    val audioFileNameWithExt: String,
    val title: String,
    val thumbnailUrl: String,
    val duration: String,
    val uploader: String,
    val description: String,
)
