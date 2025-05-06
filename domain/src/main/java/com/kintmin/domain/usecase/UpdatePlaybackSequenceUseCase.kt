package com.kintmin.domain.usecase

import com.kintmin.domain.internal_usecase.UpdatePlaylistImageWhenUpdatePlaybackUseCase
import com.kintmin.domain.repository.PlaybackRepository
import javax.inject.Inject

class UpdatePlaybackSequenceUseCase @Inject constructor(
    private val playbackRepository: PlaybackRepository,
    private val updatePlaylistImageWhenUpdatePlaybackUseCase: UpdatePlaylistImageWhenUpdatePlaybackUseCase,
) {
    suspend operator fun invoke(playlistId: Int, audioMediaId: Int, newSequence: Int) = runCatching {
        playbackRepository.updatePlaybackSequence(playlistId, audioMediaId, newSequence)
        updatePlaylistImageWhenUpdatePlaybackUseCase(playlistId)
    }
}