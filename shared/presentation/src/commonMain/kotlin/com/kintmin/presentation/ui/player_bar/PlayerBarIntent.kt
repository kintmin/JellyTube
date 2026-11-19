package com.kintmin.presentation.ui.player_bar

sealed interface PlayerBarIntent {
    data object OnClickPlayOrPauseButton: PlayerBarIntent
    data object OnRefreshMediaData: PlayerBarIntent
}
