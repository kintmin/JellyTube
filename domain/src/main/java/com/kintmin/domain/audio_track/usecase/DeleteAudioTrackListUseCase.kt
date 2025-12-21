package com.kintmin.domain.audio_track.usecase

import com.kintmin.domain.audio_track.repository.AudioTrackRepository
import javax.inject.Inject

class DeleteAudioTrackListUseCase @Inject constructor(
    private val audioTrackRepository: AudioTrackRepository,
) {
    suspend operator fun invoke(playlistId: Int, audioMediaIdList: List<Int>): Result<Unit> {
        return audioTrackRepository.deleteCustomAudioTrack(playlistId, audioMediaIdList)
    }
}