package com.kintmin.domain.audio_track.usecase

import com.kintmin.domain.audio_track.repository.AudioTrackRepository

class AddAudioMediaListToPlaylistUseCase constructor(
    private val audioTrackRepository: AudioTrackRepository,
) {
    suspend operator fun invoke(playlistId: Int, audioMediaIdList: List<Int>): Result<Int> {
        return audioTrackRepository.addCustomAudioTrack(playlistId, audioMediaIdList)
    }
}