package com.kintmin.presentation.ui.secret_playlist

import com.kintmin.presentation.ui.main.playlist.PlaylistItemUiState
import kotlin.time.Duration

data class SecretPlaylistUiState(
    val isEnabled: Boolean = false,
    val minutes: Int = 30,
    val remaining: Duration? = null,
    val playlists: List<PlaylistItemUiState> = emptyList(),
)
