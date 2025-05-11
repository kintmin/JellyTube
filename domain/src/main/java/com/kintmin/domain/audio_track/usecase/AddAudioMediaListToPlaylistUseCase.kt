package com.kintmin.domain.audio_track.usecase

import com.kintmin.domain.audio_track.repository.AudioTrackRepository
import com.kintmin.domain.playlist.model.Playlist
import com.kintmin.domain.playlist.usecase.UpdatePlaylistCountAndPlayTimeWhenUpdatePlaybackUseCase
import com.kintmin.domain.playlist.usecase.UpdatePlaylistImageWhenUpdateTrackUseCase
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.withContext
import javax.inject.Inject

class AddAudioMediaListToPlaylistUseCase @Inject constructor(
    private val audioTrackRepository: AudioTrackRepository,
    private val updatePlaylistCountAndPlayTimeWhenUpdatePlaybackUseCase: UpdatePlaylistCountAndPlayTimeWhenUpdatePlaybackUseCase,
    private val updatePlaylistImageWhenUpdateTrackUseCase: UpdatePlaylistImageWhenUpdateTrackUseCase,
) {
    suspend operator fun invoke(playlistId: Int, audioMediaIdList: List<Int>): Result<Unit> {
        return runCatching {
            withContext(Dispatchers.IO) {
                audioTrackRepository.addAudioTrackList(playlistId, audioMediaIdList).onSuccess {
                    listOf(
                        async { updatePlaylistCountAndPlayTimeWhenUpdatePlaybackUseCase(playlistId) },
                        async { updatePlaylistImageWhenUpdateTrackUseCase(playlistId) }
                    ).awaitAll()

                    audioTrackRepository.deleteAudioTrackList(Playlist.UNCATEGORIZED, audioMediaIdList)

                    listOf(
                        async { updatePlaylistCountAndPlayTimeWhenUpdatePlaybackUseCase(Playlist.UNCATEGORIZED) },
                        async { updatePlaylistImageWhenUpdateTrackUseCase(Playlist.UNCATEGORIZED) }
                    ).awaitAll()
                }.getOrThrow()
            }
        }
    }
}