package com.kintmin.presentation.ui.player_detail

sealed interface PlayerDetailIntent {
    data object OnRefreshMediaData : PlayerDetailIntent
    data object OnClickPlayOrPauseButton : PlayerDetailIntent
    data object OnClickPreviousMediaButton : PlayerDetailIntent
    data object OnClickNextMediaButton : PlayerDetailIntent
    data class OnChangeTimeSlider(val duration: Float) : PlayerDetailIntent
    data object OnChangeFinishTimeSlider : PlayerDetailIntent
}
