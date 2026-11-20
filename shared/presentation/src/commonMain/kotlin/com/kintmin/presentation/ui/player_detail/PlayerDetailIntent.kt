package com.kintmin.presentation.ui.player_detail

sealed interface PlayerDetailIntent {
    data object OnRefreshMediaData : PlayerDetailIntent
    data object OnClickPlayOrPauseButton : PlayerDetailIntent
    data object OnClickPreviousMediaButton : PlayerDetailIntent
    data object OnClickNextMediaButton : PlayerDetailIntent
    data object OnClickShuffleButton : PlayerDetailIntent
    data object OnClickRepeatButton : PlayerDetailIntent
    data object OnClickRepeatRangeButton : PlayerDetailIntent
    data object OnClickFavoriteButton : PlayerDetailIntent
    data object OnClickArtwork : PlayerDetailIntent
    data object OnClickLyricLine : PlayerDetailIntent
    data object OnClickAddButton : PlayerDetailIntent
    data object OnClickMoreButton : PlayerDetailIntent
    data object OnClickPlayingPlaylistButton : PlayerDetailIntent
    data object OnClickPlaybackSpeedButton : PlayerDetailIntent
    data object OnDismissPlaybackSpeedMenu : PlayerDetailIntent
    data class OnSelectPlaybackSpeed(val speed: Float) : PlayerDetailIntent
    data object OnClickPlaybackPitchButton : PlayerDetailIntent
    data object OnDismissPlaybackPitchMenu : PlayerDetailIntent
    data class OnSelectPlaybackPitchSemitone(val semitone: Int) : PlayerDetailIntent
    data class OnChangeTimeSlider(val duration: Float) : PlayerDetailIntent
    data object OnChangeFinishTimeSlider : PlayerDetailIntent
    data object OnClickSleepTimerButton : PlayerDetailIntent
    data object OnDismissSleepTimerMenu : PlayerDetailIntent
    data class OnSelectSleepTimerMinutes(val minutes: Int) : PlayerDetailIntent
    data object OnStartSleepTimer : PlayerDetailIntent
    data object OnCancelSleepTimer : PlayerDetailIntent
    data object OnClickSleepTimerAnnouncementButton : PlayerDetailIntent
    data object OnDismissSleepTimerAnnouncementMenu : PlayerDetailIntent
    data class OnSelectSleepTimerAnnouncement(val mediaId: Int?) : PlayerDetailIntent
}
