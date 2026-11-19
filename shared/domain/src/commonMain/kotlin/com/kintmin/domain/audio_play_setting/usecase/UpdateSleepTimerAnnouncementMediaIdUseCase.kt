package com.kintmin.domain.audio_play_setting.usecase

import com.kintmin.domain.audio_play_setting.repository.AudioPlaySettingRepository

class UpdateSleepTimerAnnouncementMediaIdUseCase constructor(
    private val audioPlaySettingRepository: AudioPlaySettingRepository,
) {
    suspend operator fun invoke(mediaId: Int?): Result<Unit> {
        return audioPlaySettingRepository.updateSleepTimerAnnouncementMediaId(mediaId)
    }
}
