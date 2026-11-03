package com.kintmin.presentation.port

import com.kintmin.domain.audio_edit.model.AudioEditTimeline

/** 음원 편집본 저장을 백그라운드 작업으로 넘긴다. */
interface ExecuteAudioEditSave {

    /** 편집본을 만들어 이 음원에 적용한다. */
    fun overwrite(audioMediaId: Int, timeline: AudioEditTimeline): Result<Unit>

    /** 편집본을 원본과 별개인 새 음원으로 저장한다. */
    fun saveAsNewMedia(audioMediaId: Int, timeline: AudioEditTimeline, name: String): Result<Unit>
}
