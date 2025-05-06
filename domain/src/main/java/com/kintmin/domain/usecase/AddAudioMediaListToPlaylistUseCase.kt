package com.kintmin.domain.usecase

import com.kintmin.domain.internal_usecase.UpdatePlaylistAfterUpdatePlaybackUseCase
import com.kintmin.domain.model.Playlist
import com.kintmin.domain.repository.PlaybackRepository
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import javax.inject.Inject

class AddAudioMediaListToPlaylistUseCase @Inject constructor(
    private val playbackRepository: PlaybackRepository,
    private val updatePlaylistAfterUpdatePlaybackUseCase: UpdatePlaylistAfterUpdatePlaybackUseCase,
) {
    suspend operator fun invoke(playlistId: Int, audioMediaIdList: List<Int>): Result<Unit> {
        return runCatching {
            coroutineScope {
                playbackRepository.addAudioMediaListToPlaylist(playlistId, audioMediaIdList).onSuccess {
                    updatePlaylistAfterUpdatePlaybackUseCase(playlistId)

                    audioMediaIdList.map { audioMediaId ->
                        async { playbackRepository.deletePlaylistTrack(Playlist.UNCATEGORIZED, audioMediaId) }
                    }.awaitAll()

                    updatePlaylistAfterUpdatePlaybackUseCase(Playlist.UNCATEGORIZED)
                }.getOrThrow()
            }
        }
    }
}