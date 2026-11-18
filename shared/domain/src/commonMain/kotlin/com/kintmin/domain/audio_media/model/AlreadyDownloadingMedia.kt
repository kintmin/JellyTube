package com.kintmin.domain.audio_media.model

class AlreadyDownloadingMedia(
    override val message: String = "같은 출처의 미디어가 현재 다운로드중입니다."
) : Throwable()
