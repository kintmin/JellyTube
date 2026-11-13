package com.kintmin.domain.audio_edit.model

class AudioEditAlreadySaving(
    override val message: String = "이미 편집본을 저장하고 있습니다. 끝난 뒤 다시 시도해주세요."
) : Throwable()
