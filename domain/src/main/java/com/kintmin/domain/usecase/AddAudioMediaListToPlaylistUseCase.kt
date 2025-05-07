package com.kintmin.domain.usecase

import com.kintmin.domain.internal_usecase.UpdatePlaylistCountAndPlayTimeWhenUpdatePlaybackUseCase
import com.kintmin.domain.internal_usecase.UpdatePlaylistImageWhenUpdatePlaybackUseCase
import com.kintmin.domain.model.Playlist
import com.kintmin.domain.repository.PlaybackRepository
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import javax.inject.Inject

class AddAudioMediaListToPlaylistUseCase @Inject constructor(
    private val playbackRepository: PlaybackRepository,
    private val updatePlaylistCountAndPlayTimeWhenUpdatePlaybackUseCase: UpdatePlaylistCountAndPlayTimeWhenUpdatePlaybackUseCase,
    private val updatePlaylistImageWhenUpdatePlaybackUseCase: UpdatePlaylistImageWhenUpdatePlaybackUseCase,
) {
    suspend operator fun invoke(playlistId: Int, audioMediaIdList: List<Int>): Result<Unit> {
        return runCatching {
            coroutineScope {
                playbackRepository.addAudioMediaListToPlaylist(playlistId, audioMediaIdList).onSuccess {
                    listOf(
                        async { updatePlaylistCountAndPlayTimeWhenUpdatePlaybackUseCase(playlistId) },
                        async { updatePlaylistImageWhenUpdatePlaybackUseCase(playlistId) }
                    ).awaitAll()

                    audioMediaIdList.map { audioMediaId ->
                        async { playbackRepository.deletePlaylistTrackMedia(Playlist.UNCATEGORIZED, audioMediaId) }
                    }.awaitAll()

                    listOf(
                        async { updatePlaylistCountAndPlayTimeWhenUpdatePlaybackUseCase(Playlist.UNCATEGORIZED) },
                        async { updatePlaylistImageWhenUpdatePlaybackUseCase(Playlist.UNCATEGORIZED) }
                    ).awaitAll()
                }.getOrThrow()
            }
        }
    }
}