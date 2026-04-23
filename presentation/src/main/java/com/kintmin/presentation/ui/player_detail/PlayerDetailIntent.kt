package com.kintmin.presentation.ui.player_detail

sealed interface PlayerDetailIntent {
    data object OnRefreshMediaData : PlayerDetailIntent
    data object OnClickPlayOrPauseButton : PlayerDetailIntent
    data object OnClickPreviousMediaButton : PlayerDetailIntent
    data object OnClickNextMediaButton : PlayerDetailIntent
    data object OnClickShuffleButton : PlayerDetailIntent
    data object OnClickRepeatButton : PlayerDetailIntent
    data object OnClickAddButton : PlayerDetailIntent
    data object OnClickMoreButton : PlayerDetailIntent
    data object OnClickPlayingPlaylistButton : PlayerDetailIntent
    data class OnChangeTimeSlider(val duration: Float) : PlayerDetailIntent
    data object OnChangeFinishTimeSlider : PlayerDetailIntent
}
