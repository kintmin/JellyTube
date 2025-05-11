package com.kintmin.domain.audio_track.usecase

import com.kintmin.domain.audio_track.repository.AudioTrackRepository
import com.kintmin.domain.playlist.usecase.UpdatePlaylistImageWhenUpdateTrackUseCase
import javax.inject.Inject

class UpdateTrackSequenceUseCase @Inject constructor(
    private val audioTrackRepository: AudioTrackRepository,
    private val updatePlaylistImageWhenUpdateTrackUseCase: UpdatePlaylistImageWhenUpdateTrackUseCase,
) {
    suspend operator fun invoke(playlistId: Int, audioMediaId: Int, newSequence: Int) = runCatching {
        audioTrackRepository.updateTrackSequence(playlistId, audioMediaId, newSequence)
        updatePlaylistImageWhenUpdateTrackUseCase(playlistId)
    }
}