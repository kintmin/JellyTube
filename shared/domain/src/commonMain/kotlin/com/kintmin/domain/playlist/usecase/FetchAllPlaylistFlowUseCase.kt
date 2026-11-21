package com.kintmin.domain.playlist.usecase

import com.kintmin.domain.audio_track.repository.AudioTrackRepository
import com.kintmin.domain.playlist.model.Playlist
import com.kintmin.domain.playlist.repository.PlaylistRepository
import com.kintmin.domain.secret_mode.usecase.FetchSecretModeEnabledFlowUseCase
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.IO
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flowOn

/** 비밀모드가 꺼져 있으면 비밀 플레이리스트를 빼고 돌려준다. */
class FetchAllPlaylistFlowUseCase constructor(
    private val playlistRepository: PlaylistRepository,
    private val audioTrackRepository: AudioTrackRepository,
    private val fetchSecretModeEnabledFlowUseCase: FetchSecretModeEnabledFlowUseCase,
) {
    operator fun invoke(): Flow<List<Playlist>> {
        return combine(
            playlistRepository.getAllPlaylistFlow(),
            audioTrackRepository.getSecretTrackListFlow(),
            fetchSecretModeEnabledFlowUseCase(),
        ) { playlists, secretTracks, isSecretModeEnabled ->
            playlists
                .filter { it.type.isVisible(isSecretModeEnabled) }
                .map { it.hidingSecretTracks(secretTracks) }
        }.flowOn(Dispatchers.IO)
    }
}
