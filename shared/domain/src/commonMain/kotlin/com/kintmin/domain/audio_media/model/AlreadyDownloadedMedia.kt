package com.kintmin.domain.audio_media.model

class AlreadyDownloadedMedia(
    override val message: String = "이미 저장된 미디어입니다."
) : Throwable()
