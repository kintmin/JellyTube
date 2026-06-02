package com.kintmin.domain.audio_play_setting.usecase

import com.kintmin.domain.audio_play_setting.repository.AudioPlaySettingRepository

class UpdatePlaybackSpeedUseCase constructor(
    private val audioPlaySettingRepository: AudioPlaySettingRepository,
) {
    suspend operator fun invoke(speed: Float): Result<Unit> {
        return audioPlaySettingRepository.updatePlaybackSpeed(speed)
    }
}
