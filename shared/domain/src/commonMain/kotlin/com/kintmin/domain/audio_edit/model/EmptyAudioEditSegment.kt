package com.kintmin.domain.audio_edit.model

class EmptyAudioEditSegment(
    override val message: String = "소리가 남은 조각이 하나 이상 있어야 합니다."
) : Throwable()
