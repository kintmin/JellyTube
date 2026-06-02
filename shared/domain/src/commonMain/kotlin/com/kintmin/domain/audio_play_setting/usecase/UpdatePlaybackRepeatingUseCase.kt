package com.kintmin.domain.audio_play_setting.usecase

import com.kintmin.domain.audio_play_setting.repository.AudioPlaySettingRepository

class UpdatePlaybackRepeatingUseCase constructor(
    private val audioPlaySettingRepository: AudioPlaySettingRepository,
) {
    suspend operator fun invoke(isRepeating: Boolean): Result<Unit> {
        return audioPlaySettingRepository.updateIsPlaybackRepeating(isRepeating)
    }
}