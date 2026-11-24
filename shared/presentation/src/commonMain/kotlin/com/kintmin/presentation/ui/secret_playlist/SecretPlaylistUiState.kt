package com.kintmin.presentation.ui.secret_playlist

import com.kintmin.domain.secret_mode.repository.SecretModeRepository
import com.kintmin.presentation.ui.main.playlist.PlaylistItemUiState
import kotlin.time.Duration

data class SecretPlaylistUiState(
    val isEnabled: Boolean = false,
    val minutes: Int = SecretModeRepository.DEFAULT_MINUTES,
    val remaining: Duration? = null,
    val playlists: List<PlaylistItemUiState> = emptyList(),
    /** 즐겨찾기에 담긴 비밀 곡. 메인 즐겨찾기에서는 빠지므로 여기가 그것을 보는 유일한 자리다. */
    val secretFavorite: PlaylistItemUiState? = null,
    val isReorderBottomSheetVisible: Boolean = false,
)
