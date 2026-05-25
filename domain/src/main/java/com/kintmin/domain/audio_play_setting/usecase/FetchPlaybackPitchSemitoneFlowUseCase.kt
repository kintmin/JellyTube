package com.kintmin.domain.audio_play_setting.usecase

import com.kintmin.domain.audio_play_setting.repository.AudioPlaySettingRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

class FetchPlaybackPitchSemitoneFlowUseCase @Inject constructor(
    private val audioPlaySettingRepository: AudioPlaySettingRepository,
) {
    operator fun invoke(): Flow<Int> {
        return audioPlaySettingRepository.getPlaybackPitchSemitoneFlow()
    }
}
