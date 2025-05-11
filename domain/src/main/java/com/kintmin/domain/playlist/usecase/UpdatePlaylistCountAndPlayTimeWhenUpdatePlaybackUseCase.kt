package com.kintmin.domain.playlist.usecase

import com.kintmin.domain.audio_track.repository.AudioTrackRepository
import com.kintmin.domain.playlist.repository.PlaylistRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext
import javax.inject.Inject

class UpdatePlaylistCountAndPlayTimeWhenUpdatePlaybackUseCase @Inject constructor(
    private val playlistRepository: PlaylistRepository,
    private val audioTrackRepository: AudioTrackRepository,
) {
    suspend operator fun invoke(playlistId: Int) {
        withContext(Dispatchers.IO) {
            val totalList = audioTrackRepository.getPlaylistTrackAggregateListFlow(playlistId).first()
            val currentCount = totalList.count()
            val currentPlayTimeDuration = totalList.sumOf { it.audioMedia.audioDuration?.inWholeSeconds ?: 0L }

            playlistRepository.updatePlaylist(
                id = playlistId,
                audioMediaCount = currentCount,
                rawTotalDuration = currentPlayTimeDuration,
            ).getOrThrow()
        }
    }
}