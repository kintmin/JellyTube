package com.kintmin.domain.data_migration.usecase

import com.kintmin.domain.playlist.model.Playlist
import com.kintmin.domain.playlist.model.PlaylistType
import com.kintmin.domain.playlist.usecase.FetchAllPlaylistFlowUseCase
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

/**
 * 플레이리스트 단위로 보낼 수 있는 것. 비밀 플레이리스트는 비밀모드가 켜져 있을 때만 들어온다.
 *
 * 전체는 보내는 단위가 아니다 — 모든 곡을 담은 것이라 플레이리스트가 아니라 전체선택으로 보낸다.
 */
class FetchShareablePlaylistListFlowUseCase constructor(
    private val fetchAllPlaylistFlowUseCase: FetchAllPlaylistFlowUseCase,
) {
    operator fun invoke(): Flow<List<Playlist>> =
        fetchAllPlaylistFlowUseCase().map { list -> list.filterNot { it.type == PlaylistType.TOTAL } }
}
