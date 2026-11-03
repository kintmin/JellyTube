package com.kintmin.presentation.ui.player_bar

sealed interface PlayerBarIntent {
    data object OnClickPlayOrPauseButton: PlayerBarIntent
    data class OnChangeTimeSlider(val duration: Float): PlayerBarIntent
    data object OnChangeFinishTimeSlider: PlayerBarIntent
    data object OnRefreshMediaData: PlayerBarIntent
}