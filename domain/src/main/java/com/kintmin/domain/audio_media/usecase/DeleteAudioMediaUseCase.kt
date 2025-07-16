package com.kintmin.domain.audio_media.usecase

import com.kintmin.domain.audio_media.repository.AudioMediaRepository
import com.kintmin.domain.playlist.model.Playlist
import com.kintmin.domain.playlist.usecase.UpdatePlaylistCountAndPlayTimeWhenUpdatePlaybackUseCase
import com.kintmin.domain.playlist.usecase.UpdatePlaylistImageWhenUpdateTrackUseCase
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.joinAll
import kotlinx.coroutines.launch
import kotlinx.coroutines.supervisorScope
import kotlinx.coroutines.withContext
import javax.inject.Inject

class DeleteAudioMediaUseCase @Inject constructor(
    private val audioMediaRepository: AudioMediaRepository,
    private val updatePlaylistCountAndPlayTimeWhenUpdatePlaybackUseCase: UpdatePlaylistCountAndPlayTimeWhenUpdatePlaybackUseCase,
    private val updatePlaylistImageWhenUpdateTrackUseCase: UpdatePlaylistImageWhenUpdateTrackUseCase,
) {
    suspend operator fun invoke(audioMediaId: Int): Result<Unit> = runCatching {
        withContext(Dispatchers.IO) {
            val targetPlaylistIdList = audioMediaRepository.deleteAudioMedia(audioMediaId).getOrThrow()

            supervisorScope {
                targetPlaylistIdList.flatMap  { playlistId ->
                    listOf(
                        launch { updatePlaylistCountAndPlayTimeWhenUpdatePlaybackUseCase(playlistId) },
                        launch { updatePlaylistImageWhenUpdateTrackUseCase(playlistId) },
                    )
                }.joinAll()
            }

            supervisorScope {
                listOf(
                    launch { updatePlaylistCountAndPlayTimeWhenUpdatePlaybackUseCase(Playlist.TOTAL) },
                    launch { updatePlaylistImageWhenUpdateTrackUseCase(Playlist.TOTAL) },
                    launch { updatePlaylistCountAndPlayTimeWhenUpdatePlaybackUseCase(Playlist.UNCATEGORIZED) },
                    launch { updatePlaylistImageWhenUpdateTrackUseCase(Playlist.UNCATEGORIZED) },
                ).joinAll()
            }
        }
    }
}