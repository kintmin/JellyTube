package com.kintmin.domain.playlist.usecase

import com.kintmin.domain.playlist.model.Playlist
import com.kintmin.domain.playlist.repository.PlaylistRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.IO
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOn

class FetchPlaylistFlowUseCase  constructor(
    private val playlistRepository: PlaylistRepository,
){
    operator fun invoke(playlistId: Int): Flow<Playlist> {
        return playlistRepository.getPlaylistFlow(playlistId).flowOn(Dispatchers.IO)
    }
}