package com.kintmin.domain.playlist.usecase

import com.kintmin.domain.playlist.model.Playlist
import com.kintmin.domain.playlist.repository.PlaylistRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import javax.inject.Inject

class FetchAllPlaylistFlowUseCase @Inject constructor(
    private val playlistRepository: PlaylistRepository,
) {
    operator fun invoke(): Flow<List<Playlist>> {
        return playlistRepository.getAllPlaylistFlow()
            .flowOn(Dispatchers.IO)
            .map { playlistList ->
                // 미분류는 분류된 미디어가 없으면 fetch하지 않는다.
                playlistList.filterNot { it.id == Playlist.UNCATEGORIZED && it.audioMediaCount == 0 }
            }
    }
}