package com.kintmin.domain.playlist.usecase

import com.kintmin.domain.app_setting.usecase.FetchPlaylistIdOnDownloadFlowUseCase
import com.kintmin.domain.playlist.model.DownloadTargetPlaylists
import com.kintmin.domain.playlist.model.Playlist
import com.kintmin.domain.playlist.model.PlaylistType
import com.kintmin.domain.secret_mode.usecase.FetchSecretModeEnabledFlowUseCase
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine

class FetchDownloadTargetPlaylistsFlowUseCase(
    private val fetchPlaylistIdOnDownloadFlowUseCase: FetchPlaylistIdOnDownloadFlowUseCase,
    private val fetchAllPlaylistFlowUseCase: FetchAllPlaylistFlowUseCase,
    private val fetchSecretModeEnabledFlowUseCase: FetchSecretModeEnabledFlowUseCase,
) {
    operator fun invoke(): Flow<DownloadTargetPlaylists> = combine(
        fetchPlaylistIdOnDownloadFlowUseCase(),
        fetchAllPlaylistFlowUseCase(),
        fetchSecretModeEnabledFlowUseCase(),
        ::resolve,
    )

    internal fun resolve(savedId: Int?, playlists: List<Playlist>, isSecretModeEnabled: Boolean): DownloadTargetPlaylists {
        val selectable = playlists.filter {
            it.type.canBeDownloadTarget && (isSecretModeEnabled || it.type != PlaylistType.SECRET)
        }
        val targetId = selectable.firstOrNull { it.id == savedId }?.id
            ?: selectable.firstOrNull { it.type == PlaylistType.UNCATEGORIZED }?.id
        return DownloadTargetPlaylists(targetId, selectable)
    }
}
