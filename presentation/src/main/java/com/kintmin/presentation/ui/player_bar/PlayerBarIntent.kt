package com.kintmin.presentation.ui.player_bar

interface PlayerBarIntent {
    data object OnClickPlayOrPauseButton: PlayerBarIntent
    data class OnChangeTimeSlider(val duration: Float): PlayerBarIntent
}