package com.kintmin.domain.audio_play_setting.usecase

import com.kintmin.domain.audio_play_setting.repository.AudioPlaySettingRepository

class UpdatePlaybackPitchSemitoneUseCase constructor(
    private val audioPlaySettingRepository: AudioPlaySettingRepository,
) {
    suspend operator fun invoke(semitone: Int): Result<Unit> {
        return audioPlaySettingRepository.updatePlaybackPitchSemitone(semitone)
    }
}
