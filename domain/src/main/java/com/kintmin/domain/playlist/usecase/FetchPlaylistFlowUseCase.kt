package com.kintmin.domain.playlist.usecase

import com.kintmin.domain.playlist.model.Playlist
import com.kintmin.domain.playlist.repository.PlaylistRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

class FetchPlaylistFlowUseCase  @Inject constructor(
    private val playlistRepository: PlaylistRepository,
){
    operator fun invoke(playlistId: Int): Flow<Playlist> {
        return playlistRepository.getPlaylistFlow(playlistId)
    }
}