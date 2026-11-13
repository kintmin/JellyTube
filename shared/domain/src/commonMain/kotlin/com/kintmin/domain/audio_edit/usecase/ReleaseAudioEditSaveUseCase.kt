package com.kintmin.domain.audio_edit.usecase

import com.kintmin.domain.audio_edit.repository.AudioEditRepository

/** 저장을 맡기지 못했거나 저장이 시작 전에 끝났을 때 예약을 푼다. */
class ReleaseAudioEditSaveUseCase constructor(
    private val audioEditRepository: AudioEditRepository,
) {
    operator fun invoke(audioMediaId: Int) = audioEditRepository.releaseSaving(audioMediaId)
}
