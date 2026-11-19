package com.kintmin.domain.audio_play_setting.usecase

import com.kintmin.domain.audio_play_setting.repository.AudioPlaySettingRepository

class UpdateSleepTimerMinutesUseCase constructor(
    private val audioPlaySettingRepository: AudioPlaySettingRepository,
) {
    suspend operator fun invoke(minutes: Int): Result<Unit> {
        return audioPlaySettingRepository.updateSleepTimerMinutes(minutes)
    }
}
