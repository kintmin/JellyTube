package com.kintmin.domain.playlist.usecase

import com.kintmin.domain.audio_track.repository.AudioTrackRepository
import com.kintmin.domain.playlist.model.Playlist
import com.kintmin.domain.playlist.repository.PlaylistRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.IO
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flowOn

class FetchAllPlaylistFlowUseCase constructor(
    private val playlistRepository: PlaylistRepository,
    private val audioTrackRepository: AudioTrackRepository,
) {
    operator fun invoke(): Flow<List<Playlist>> {
        return combine(playlistRepository.getAllPlaylistFlow(), audioTrackRepository.getSecretTrackListFlow()) { playlists, secretTracks ->
            playlists.map { it.hidingSecretTracks(secretTracks) }
        }.flowOn(Dispatchers.IO)
    }
}