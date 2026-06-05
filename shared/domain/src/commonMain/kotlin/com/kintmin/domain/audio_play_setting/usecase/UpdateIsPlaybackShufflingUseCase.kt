package com.kintmin.domain.audio_play_setting.usecase

import com.kintmin.domain.audio_play_setting.repository.AudioPlaySettingRepository

class UpdateIsPlaybackShufflingUseCase constructor(
    private val audioPlaySettingRepository: AudioPlaySettingRepository,
) {
    suspend operator fun invoke(isShuffling: Boolean): Result<Unit> {
        return audioPlaySettingRepository.updateIsPlaybackShuffling(isShuffling)
    }
}