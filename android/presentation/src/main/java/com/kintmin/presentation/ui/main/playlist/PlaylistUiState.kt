package com.kintmin.presentation.ui.main.playlist

import com.kintmin.domain.playlist.model.Playlist
import com.kintmin.presentation.extension.to_hh_colon_mm_colon_ss
import kotlin.time.Duration
import kotlin.time.Duration.Companion.seconds

data class PlaylistItemUiState(
    val id: Int,
    val imageFileFullPath: String?,
    val name: String,
    val description: String,
    val playlistDuration: Duration,
    val audioMediaCount: Int,
    val sequence: Int,
    val isBasePlaylist: Boolean,
) {
    val durationString: String get() = "재생시간: ${playlistDuration.to_hh_colon_mm_colon_ss()}"
    val audioMediaCountString: String get() = "음원수: $audioMediaCount"

    companion object {
        fun getMock(id: Int = 0): PlaylistItemUiState {
            return PlaylistItemUiState(
                id = id,
                imageFileFullPath = null,
                name = "새로운 플레이리스트",
                playlistDuration = 12000.seconds,
                audioMediaCount = 999,
                description = "",
                sequence = id,
                isBasePlaylist = false,
            )
        }

        fun getMockList() = List(5) { index -> getMock(index) }
    }
}

fun Playlist.toUiModel() = PlaylistItemUiState(
    id = id,
    imageFileFullPath = imageFileFullPath,
    name = name,
    description = description,
    audioMediaCount = audioMediaCount,
    playlistDuration = playTimeDuration,
    sequence = sequence,
    isBasePlaylist = isBasePlaylist,
)
