package com.kintmin.domain.audio_play_setting.usecase

import com.kintmin.domain.audio_play_setting.repository.AudioPlaySettingRepository
import kotlinx.coroutines.flow.Flow

class FetchPlaybackSpeedFlowUseCase constructor(
    private val audioPlaySettingRepository: AudioPlaySettingRepository,
) {
    operator fun invoke(): Flow<Float> {
        return audioPlaySettingRepository.getPlaybackSpeedFlow()
    }
}
