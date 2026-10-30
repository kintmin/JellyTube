package com.kintmin.domain.audio_edit.usecase

import com.kintmin.domain.audio_edit.model.AudioEditTimeline
import com.kintmin.domain.audio_edit.repository.AudioEditRepository

/** 편집본을 만든 편집 상태를 불러온다. 다시 편집할 때 이전 조각 배치에서 이어서 고칠 수 있다. */
class FetchAudioEditTimelineUseCase constructor(
    private val audioEditRepository: AudioEditRepository,
) {
    suspend operator fun invoke(audioMediaId: Int): Result<AudioEditTimeline?> =
        audioEditRepository.fetchEditTimeline(audioMediaId)
}
