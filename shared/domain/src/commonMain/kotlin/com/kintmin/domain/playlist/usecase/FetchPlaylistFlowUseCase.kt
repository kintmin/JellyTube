package com.kintmin.domain.playlist.usecase

import com.kintmin.domain.audio_track.repository.AudioTrackRepository
import com.kintmin.domain.playlist.model.Playlist
import com.kintmin.domain.playlist.model.PlaylistType
import com.kintmin.domain.playlist.repository.PlaylistRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.IO
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map

class FetchPlaylistFlowUseCase  constructor(
    private val playlistRepository: PlaylistRepository,
    private val audioTrackRepository: AudioTrackRepository,
){
    @OptIn(ExperimentalCoroutinesApi::class)
    operator fun invoke(playlistId: Int): Flow<Playlist> {
        return playlistRepository.getPlaylistFlow(playlistId).flatMapLatest { playlist ->
            if (playlist.type != PlaylistType.TOTAL) return@flatMapLatest flowOf(playlist)
            audioTrackRepository.getSecretTrackListFlow().map { playlist.hidingSecretTracks(it) }
        }.flowOn(Dispatchers.IO)
    }
}
