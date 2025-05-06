package com.kintmin.domain.usecase

import com.kintmin.domain.internal_usecase.UpdatePlaylistCountAndPlayTimeWhenUpdatePlaybackUseCase
import com.kintmin.domain.internal_usecase.UpdatePlaylistImageWhenUpdatePlaybackUseCase
import com.kintmin.domain.repository.AudioMediaRepository
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import javax.inject.Inject

class DeleteAudioMediaUseCase @Inject constructor(
    private val audioMediaRepository: AudioMediaRepository,
    private val updatePlaylistCountAndPlayTimeWhenUpdatePlaybackUseCase: UpdatePlaylistCountAndPlayTimeWhenUpdatePlaybackUseCase,
    private val updatePlaylistImageWhenUpdatePlaybackUseCase: UpdatePlaylistImageWhenUpdatePlaybackUseCase,
) {
    suspend operator fun invoke(playlistId: Int, audioMediaId: Int): Result<Unit> = runCatching {
        coroutineScope {
            audioMediaRepository.deleteAudioMedia(audioMediaId).onSuccess {
                listOf(
                    async { updatePlaylistCountAndPlayTimeWhenUpdatePlaybackUseCase(playlistId) },
                    async { updatePlaylistImageWhenUpdatePlaybackUseCase(playlistId) },
                ).awaitAll()
            }.getOrThrow()
        }
    }
}