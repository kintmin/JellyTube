package com.kintmin.presentation.ui.player_detail

import com.kintmin.presentation.extension.to_hh_colon_mm_colon_ss
import kotlin.time.Duration
import kotlin.time.Duration.Companion.seconds

data class PlayerDetailUiState(
    val id: String,
    val playlistId: Int?,
    val playlistName: String,
    val title: String,
    val artist: String,
    val currentDuration: Duration,
    val playbackDuration: Duration,
    val imageFileFullPath: String?,
    val isPlaying: Boolean,
    val isShuffling: Boolean,
    val isRepeating: Boolean,
    val playbackSpeed: Float,
    val playbackPitchSemitone: Int,
    val repeatRangeStartDuration: Duration? = null,
    val repeatRangeEndDuration: Duration? = null,
    val isPlaybackSpeedMenuVisible: Boolean = false,
    val isPlaybackPitchMenuVisible: Boolean = false,
) {
    val timeString get() = "${currentDuration.to_hh_colon_mm_colon_ss()} / ${playbackDuration.to_hh_colon_mm_colon_ss()}"

    companion object {
        fun getMock() = PlayerDetailUiState(
            id = "temp",
            playlistId = 1,
            playlistName = "재생목록재생목록재생목록재생목록재생목록재생목록재생목록",
            title = "제목제목제목제목제목제목제목제목제목제목제목제목",
            artist = "아티스트아티스트아티스트아티스트아티스트아티스트아티스트아티스트",
            currentDuration = 90.seconds,
            playbackDuration = 300.seconds,
            imageFileFullPath = null,
            isPlaying = true,
            isShuffling = false,
            isRepeating = false,
            playbackSpeed = 1.25f,
            playbackPitchSemitone = 10,
            repeatRangeStartDuration = 60.seconds,
            repeatRangeEndDuration = 180.seconds,
            isPlaybackSpeedMenuVisible = false,
            isPlaybackPitchMenuVisible = false,
        )
    }
}
