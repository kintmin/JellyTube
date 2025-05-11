package com.kintmin.domain.playlist.usecase

import com.kintmin.domain.playlist.model.Playlist
import com.kintmin.domain.playlist.repository.PlaylistRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject

class FetchAllPlaylistFlowUseCase @Inject constructor(
    private val playlistRepository: PlaylistRepository,
) {
    operator fun invoke(): Flow<List<Playlist>> {
        return playlistRepository.getAllPlaylistFlow()
            .map { playlistList ->
                playlistList.filterNot { it.id == Playlist.UNCATEGORIZED && it.audioMediaCount == 0 }
            }
    }
}