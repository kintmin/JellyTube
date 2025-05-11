package com.kintmin.domain.audio_media.usecase

import com.kintmin.domain.audio_media.repository.AudioMediaRepository
import com.kintmin.domain.playlist.model.Playlist
import com.kintmin.domain.playlist.usecase.UpdatePlaylistCountAndPlayTimeWhenUpdatePlaybackUseCase
import com.kintmin.domain.playlist.usecase.UpdatePlaylistImageWhenUpdateTrackUseCase
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.withContext
import javax.inject.Inject

class DeleteAudioMediaUseCase @Inject constructor(
    private val audioMediaRepository: AudioMediaRepository,
    private val updatePlaylistCountAndPlayTimeWhenUpdatePlaybackUseCase: UpdatePlaylistCountAndPlayTimeWhenUpdatePlaybackUseCase,
    private val updatePlaylistImageWhenUpdateTrackUseCase: UpdatePlaylistImageWhenUpdateTrackUseCase,
) {
    suspend operator fun invoke(playlistId: Int, audioMediaId: Int): Result<Unit> = runCatching {
        withContext(Dispatchers.IO) {
            audioMediaRepository.deleteAudioMedia(audioMediaId).onSuccess {
                listOf(
                    async { updatePlaylistCountAndPlayTimeWhenUpdatePlaybackUseCase(playlistId) },
                    async { updatePlaylistImageWhenUpdateTrackUseCase(playlistId) },
                    async { updatePlaylistCountAndPlayTimeWhenUpdatePlaybackUseCase(Playlist.TOTAL) },
                    async { updatePlaylistImageWhenUpdateTrackUseCase(Playlist.TOTAL) },
                    async { updatePlaylistCountAndPlayTimeWhenUpdatePlaybackUseCase(Playlist.UNCATEGORIZED) },
                    async { updatePlaylistImageWhenUpdateTrackUseCase(Playlist.UNCATEGORIZED) },
                ).awaitAll()
            }.getOrThrow()
        }
    }
}