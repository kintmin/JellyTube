package com.kintmin.presentation.ui.player_bar

import com.kintmin.presentation.extension.to_hh_colon_mm_colon_ss
import kotlin.time.Duration
import kotlin.time.Duration.Companion.seconds

data class PlayerBarUiState(
    val id: String,
    /** 지금 곡을 어느 플레이리스트에서 듣고 있는가. 걸린 곡이 없으면 null 이다. */
    val playlistId: Int?,
    val title: String,
    val currentDuration: Duration,
    val playbackDuration: Duration,
    val imageFileFullPath: String?,
    val isPlaying: Boolean,
) {
    val timeString get() = "${currentDuration.to_hh_colon_mm_colon_ss()} / ${playbackDuration.to_hh_colon_mm_colon_ss()}"

    /** 다른 플레이리스트에서 듣는 곡은 여기서 누르면 처음부터 다시 시작하니, 이어 듣는 것처럼 보이면 안 된다. */
    fun nowPlayingMediaIdIn(playlistId: Int): Int? = id.toIntOrNull().takeIf { this.playlistId == playlistId }

    companion object {
        fun getMock(): PlayerBarUiState {
            return PlayerBarUiState(
                id = "temp",
                playlistId = 1,
                title = "제목제목제목",
                currentDuration = 130.seconds,
                playbackDuration = 4200.seconds,
                imageFileFullPath = null,
                isPlaying = true,
            )
        }
    }
}