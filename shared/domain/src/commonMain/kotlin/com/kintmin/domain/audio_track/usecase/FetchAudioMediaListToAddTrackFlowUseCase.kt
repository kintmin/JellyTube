package com.kintmin.domain.audio_track.usecase

import com.kintmin.domain.audio_track.model.PlaylistTrackAggregate
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
            fetchPlaylistsByTypeUseCase(PlaylistType.SECRET_TOTAL),
            fetchPlaylistsByTypeUseCase(PlaylistType.SECRET),
        ) { totalPlaylists, secretTotalPlaylists, secretPlaylists ->
            // 비밀 플레이리스트에 담을 때만 이미 비밀인 곡도 후보다. 일반 곡은 양쪽 모두에서 후보다.
            val isSecretTarget = secretPlaylists.any { it.id == playlistId }
            totalPlaylists.firstOrNull()?.id to secretTotalPlaylists.firstOrNull()?.id?.takeIf { isSecretTarget }
        }
            .distinctUntilChanged()
            .flatMapLatest { (totalId, secretTotalId) ->
                if (totalId == null) return@flatMapLatest flowOf(emptyList())
                val candidates = if (secretTotalId == null) {
                    audioTrackRepository.getPlaylistTrackAggregateListFlow(totalId)
                } else {
                    combine(
                        audioTrackRepository.getPlaylistTrackAggregateListFlow(totalId),
                        audioTrackRepository.getPlaylistTrackAggregateListFlow(secretTotalId),
                    ) { open, secret -> open + secret }
                }
                combine(
                    candidates,
                    audioTrackRepository.getPlaylistTrackAggregateListFlow(playlistId),
                ) { candidateList, currentList ->
                    candidateList.filter { candidate ->
                        currentList.firstOrNull { it.audioMedia.id == candidate.audioMedia.id } == null
                    }
                }
            }
            .flowOn(Dispatchers.IO)
            .distinctUntilChanged()
    }
}
