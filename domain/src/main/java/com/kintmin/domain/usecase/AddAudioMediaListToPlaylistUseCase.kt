package com.kintmin.domain.usecase

import com.kintmin.domain.model.Playlist
import com.kintmin.domain.repository.AudioMediaRepository
import com.kintmin.domain.repository.PlaybackRepository
import com.kintmin.domain.repository.PlaylistRepository
import kotlinx.coroutines.flow.first
import javax.inject.Inject

class AddAudioMediaListToPlaylistUseCase @Inject constructor(
    private val playbackRepository: PlaybackRepository,
    private val playlistRepository: PlaylistRepository,
    private val audioMediaRepository: AudioMediaRepository,
) {
    suspend operator fun invoke(playlistId: Int, audioMediaIdList: List<Int>): Result<Unit> {
        return runCatching {
            playbackRepository.addAudioMediaListToPlaylist(playlistId, audioMediaIdList).onSuccess {
                val currentAudioList = audioMediaRepository.getAudioMediaListFlow(playlistId).first()
                val currentTotalCount = currentAudioList.count()
                val currentTotalDuration = currentAudioList.sumOf { it.audioDuration?.inWholeSeconds ?: 0L }
                playlistRepository.updatePlaylistPlayback(playlistId, currentTotalCount, currentTotalDuration).getOrThrow()

                playbackRepository.deleteAudioMediaListInPlaylist(Playlist.UNCATEGORIZED, audioMediaIdList).onSuccess {
                    val uncategorizedAudioList = audioMediaRepository.getAudioMediaListFlow(Playlist.UNCATEGORIZED).first()
                    val uncategorizedCount = uncategorizedAudioList.count()
                    val uncategorizedDuration = uncategorizedAudioList.sumOf { it.audioDuration?.inWholeSeconds ?: 0L }
                    playlistRepository.updatePlaylistPlayback(Playlist.UNCATEGORIZED, uncategorizedCount, uncategorizedDuration).getOrThrow()
                }.getOrThrow()
            }.getOrThrow()
        }
    }
}