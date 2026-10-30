package com.kintmin.domain.audio_edit.usecase

import com.kintmin.domain.audio_edit.repository.AudioEditRepository

/** 편집본 저장을 맡기기 전에 저장 중으로 예약한다. 이미 저장 중이면 실패. */
class ReserveAudioEditSaveUseCase constructor(
    private val audioEditRepository: AudioEditRepository,
) {
    operator fun invoke(audioMediaId: Int): Result<Unit> =
        if (audioEditRepository.reserveSaving(audioMediaId)) {
            Result.success(Unit)
        } else {
            Result.failure(AudioEditAlreadySaving())
        }
}

/** 저장을 맡기지 못했거나 저장이 시작 전에 끝났을 때 예약을 푼다. */
class ReleaseAudioEditSaveUseCase constructor(
    private val audioEditRepository: AudioEditRepository,
) {
    operator fun invoke(audioMediaId: Int) = audioEditRepository.releaseSaving(audioMediaId)
}
