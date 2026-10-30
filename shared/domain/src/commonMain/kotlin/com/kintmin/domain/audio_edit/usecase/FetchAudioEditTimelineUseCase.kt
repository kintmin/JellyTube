package com.kintmin.domain.audio_edit.usecase

import com.kintmin.domain.audio_edit.model.AudioEditTimeline
import com.kintmin.domain.audio_edit.repository.AudioEditRepository

class FetchAudioEditTimelineUseCase constructor(
    private val audioEditRepository: AudioEditRepository,
) {
    suspend operator fun invoke(audioMediaId: Int): Result<AudioEditTimeline?> =
        audioEditRepository.fetchEditTimeline(audioMediaId)
}
