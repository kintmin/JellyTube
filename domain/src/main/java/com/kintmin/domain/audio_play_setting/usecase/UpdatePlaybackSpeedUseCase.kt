package com.kintmin.domain.audio_play_setting.usecase

import com.kintmin.domain.audio_play_setting.repository.AudioPlaySettingRepository
import javax.inject.Inject

class UpdatePlaybackSpeedUseCase @Inject constructor(
    private val audioPlaySettingRepository: AudioPlaySettingRepository,
) {
    suspend operator fun invoke(speed: Float): Result<Unit> {
        return audioPlaySettingRepository.updatePlaybackSpeed(speed)
    }
}
