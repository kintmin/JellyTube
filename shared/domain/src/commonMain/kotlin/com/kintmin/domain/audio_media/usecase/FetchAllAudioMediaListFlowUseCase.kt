package com.kintmin.domain.audio_media.usecase

import com.kintmin.domain.audio_media.model.AudioMedia
import com.kintmin.domain.audio_media.repository.AudioMediaRepository
import kotlinx.coroutines.flow.Flow

/** 재생목록과 무관하게 앱에 저장된 전체 음원을 관찰한다. */
class FetchAllAudioMediaListFlowUseCase constructor(
    private val audioMediaRepository: AudioMediaRepository,
) {
    operator fun invoke(): Flow<List<AudioMedia>> = audioMediaRepository.getAudioMediaListFlow()
}
