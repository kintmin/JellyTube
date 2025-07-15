package com.kintmin.presentation.ui.player_bar

import com.kintmin.presentation.extension.to_hh_colon_mm_colon_ss
import kotlin.time.Duration
import kotlin.time.Duration.Companion.seconds

data class PlayerBarUiState(
    val id: String,
    val title: String,
    val currentDuration: Duration,
    val playbackDuration: Duration,
    val imageFileFullPath: String?,
) {
    val timeString get() = "${currentDuration.to_hh_colon_mm_colon_ss()} / ${playbackDuration.to_hh_colon_mm_colon_ss()}"

    companion object {
        fun getMock(): PlayerBarUiState {
            return PlayerBarUiState(
                id = "",
                title = "제목제목제목",
                currentDuration = 130.seconds,
                playbackDuration = 4200.seconds,
                imageFileFullPath = null,
            )
        }
    }
}