package com.kintmin.domain.audio_play_setting.usecase

import com.kintmin.domain.audio_play_setting.repository.AudioPlaySettingRepository
import kotlinx.coroutines.flow.Flow

class FetchIsPlaybackRepeatingFlowUseCase constructor(
    private val audioPlaySettingRepository: AudioPlaySettingRepository,
) {
    operator fun invoke(): Flow<Boolean> {
        return audioPlaySettingRepository.getIsPlaybackRepeatingFlow()
    }
}