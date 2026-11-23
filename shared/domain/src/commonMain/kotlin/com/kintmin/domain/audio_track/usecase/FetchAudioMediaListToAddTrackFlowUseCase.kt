package com.kintmin.domain.audio_track.usecase

import com.kintmin.domain.audio_track.model.PlaylistTrackAggregate
import com.kintmin.domain.audio_track.model.SecretTrackFilter
import com.kintmin.domain.audio_track.repository.AudioTrackRepository
import com.kintmin.domain.playlist.model.PlaylistType
import com.kintmin.domain.playlist.usecase.FetchPlaylistsByTypeUseCase
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.IO
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.flowOn

class FetchAudioMediaListToAddTrackFlowUseCase constructor(
    private val audioTrackRepository: AudioTrackRepository,
    private val fetchPlaylistsByTypeUseCase: FetchPlaylistsByTypeUseCase,
) {
    @OptIn(ExperimentalCoroutinesApi::class)
    operator fun invoke(playlistId: Int): Flow<List<PlaylistTrackAggregate>> {
        return combine(
            fetchPlaylistsByTypeUseCase(PlaylistType.TOTAL),
            fetchPlaylistsByTypeUseCase(PlaylistType.SECRET),
        ) { totalPlaylists, secretPlaylists ->
            // 비밀 플레이리스트에 담을 때만 비밀 곡도 후보다.
            totalPlaylists.firstOrNull()?.id to secretPlaylists.any { it.id == playlistId }
        }
            .distinctUntilChanged()
            .flatMapLatest { (totalId, isSecretTarget) ->
                if (totalId == null) return@flatMapLatest flowOf(emptyList())
                combine(
                    audioTrackRepository.getPlaylistTrackAggregateListFlow(
                        playlistId = totalId,
                        secretFilter = if (isSecretTarget) SecretTrackFilter.INCLUDE else SecretTrackFilter.EXCLUDE,
                    ),
                    audioTrackRepository.getPlaylistTrackAggregateListFlow(playlistId),
                ) { totalList, currentList ->
                    totalList.filter { total -> currentList.firstOrNull { it.audioMedia.id == total.audioMedia.id } == null }
                }
            }
            .flowOn(Dispatchers.IO)
            .distinctUntilChanged()
    }
}
