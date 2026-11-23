package com.kintmin.domain.audio_media.model

/** 로그인해야 볼 수 있는 연령 제한 영상이라 다시 시도해도 받을 수 없다. */
class AgeRestrictedMedia(
    override val message: String = "연령 제한 영상입니다."
) : Exception()
