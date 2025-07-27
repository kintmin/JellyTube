package com.kintmin.domain.audio_track.usecase

import com.kintmin.domain.audio_track.repository.AudioTrackRepository
import com.kintmin.domain.playlist.model.Playlist
import com.kintmin.domain.playlist.usecase.internal.UpdateOnPlaylistChangeUseCase
import kotlinx.coroutines.joinAll
import kotlinx.coroutines.launch
import kotlinx.coroutines.supervisorScope
import javax.inject.Inject

class AddAudioMediaListToPlaylistUseCase @Inject constructor(
    private val audioTrackRepository: AudioTrackRepository,
    private val updateOnPlaylistChangeUseCase: UpdateOnPlaylistChangeUseCase,
) {
    suspend operator fun invoke(playlistId: Int, audioMediaIdList: List<Int>): Result<Unit> {
        return runCatching {
            audioTrackRepository.addAudioTrackList(playlistId, audioMediaIdList).getOrThrow()

            supervisorScope {
                listOf(
                    launch { updateOnPlaylistChangeUseCase(playlistId) },
                    launch {
                        audioTrackRepository.deleteAudioTrackList(Playlist.UNCATEGORIZED, audioMediaIdList)
                        updateOnPlaylistChangeUseCase(Playlist.UNCATEGORIZED)
                    }
                ).joinAll()
            }
        }
    }
}