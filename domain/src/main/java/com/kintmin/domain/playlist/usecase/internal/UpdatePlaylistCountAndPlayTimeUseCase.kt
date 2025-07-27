package com.kintmin.domain.playlist.usecase.internal

import com.kintmin.domain.audio_track.repository.AudioTrackRepository
import com.kintmin.domain.playlist.repository.PlaylistRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOn
import javax.inject.Inject

class UpdatePlaylistCountAndPlayTimeUseCase @Inject constructor(
    private val playlistRepository: PlaylistRepository,
    private val audioTrackRepository: AudioTrackRepository,
) {
    suspend operator fun invoke(playlistId: Int): Result<Unit> {
        return runCatching {
            // 플레이리스트 전체 가져와서 개수 및 플레이타임 총합 계산
            val totalList = audioTrackRepository.getPlaylistTrackAggregateListFlow(playlistId)
                .flowOn(Dispatchers.IO)
                .first()
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