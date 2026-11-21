package com.kintmin.presentation.ui.secret_playlist

import com.kintmin.presentation.ui.main.playlist.PlaylistIntent

sealed interface SecretPlaylistIntent {
    data class OnToggleSecretMode(val isEnabled: Boolean) : SecretPlaylistIntent
    data class OnSelectMinutes(val minutes: Int) : SecretPlaylistIntent

    /** 플레이리스트 카드를 홈과 같이 쓰므로 그 intent 를 그대로 받는다. */
    data class OnPlaylistIntent(val intent: PlaylistIntent) : SecretPlaylistIntent
}
