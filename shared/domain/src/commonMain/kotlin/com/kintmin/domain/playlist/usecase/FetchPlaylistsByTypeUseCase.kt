package com.kintmin.domain.playlist.usecase

import com.kintmin.domain.playlist.model.Playlist
import com.kintmin.domain.playlist.model.PlaylistType
import com.kintmin.domain.playlist.repository.PlaylistRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

// 전체 플레이리스트에서 특정 type만 필터해 반환한다. (전체/미분류/즐겨찾기 조회 등)
class FetchPlaylistsByTypeUseCase constructor(
    private val playlistRepository: PlaylistRepository,
) {
    operator fun invoke(type: PlaylistType): Flow<List<Playlist>> =
        playlistRepository.getAllPlaylistFlow().map { list -> list.filter { it.type == type } }
}
