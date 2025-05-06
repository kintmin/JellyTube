package com.kintmin.domain.usecase

import com.kintmin.domain.internal_usecase.UpdatePlaylistAfterUpdatePlaybackUseCase
import com.kintmin.domain.repository.AudioMediaRepository
import com.kintmin.domain.repository.PlaylistRepository
import javax.inject.Inject

class DeleteAudioMediaUseCase @Inject constructor(
    private val audioMediaRepository: AudioMediaRepository,
    private val updatePlaylistAfterUpdatePlaybackUseCase: UpdatePlaylistAfterUpdatePlaybackUseCase,
) {
    suspend operator fun invoke(playlistId: Int, audioMediaId: Int): Result<Unit> = runCatching {
        audioMediaRepository.deleteAudioMedia(audioMediaId).onSuccess {
            updatePlaylistAfterUpdatePlaybackUseCase(playlistId)
        }.getOrThrow()
    }
}