package com.kintmin.domain.audio_track.usecase

import com.kintmin.domain.audio_track.repository.AudioTrackRepository

class DeleteAudioTrackListUseCase constructor(
    private val audioTrackRepository: AudioTrackRepository,
) {
    suspend operator fun invoke(playlistId: Int, audioMediaIdList: List<Int>): Result<Unit> {
        return audioTrackRepository.deleteCustomAudioTrack(playlistId, audioMediaIdList)
    }
}