package com.kintmin.domain.secret_mode.usecase

import com.kintmin.domain.audio_track.model.SecretTrack
import com.kintmin.domain.audio_track.repository.AudioTrackRepository
import kotlinx.coroutines.flow.Flow

class FetchSecretTrackListFlowUseCase constructor(
    private val audioTrackRepository: AudioTrackRepository,
) {
    operator fun invoke(): Flow<List<SecretTrack>> = audioTrackRepository.getSecretTrackListFlow()
}
