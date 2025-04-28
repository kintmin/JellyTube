package com.kintmin.domain.usecase

import com.kintmin.domain.model.Playlist
import com.kintmin.domain.repository.PlaylistRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject

class FetchPlaylistListFlowUseCase @Inject constructor(
    private val playlistRepository: PlaylistRepository,
) {
    operator fun invoke(): Flow<List<Playlist>> {
        return playlistRepository.getPlaylistListFlow()
            .map { playlistList ->
                playlistList.filterNot { it.id == Playlist.UNCATEGORIZED && it.audioMediaCount == 0 }
            }
    }
}