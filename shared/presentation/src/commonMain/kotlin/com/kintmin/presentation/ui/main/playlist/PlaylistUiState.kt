package com.kintmin.presentation.ui.main.playlist

import com.kintmin.domain.playlist.model.Playlist
import com.kintmin.domain.playlist.model.PlaylistType
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
    val type: PlaylistType,
) {
    val isBasePlaylist: Boolean get() = type.isSystem
    val summaryString: String get() = "${audioMediaCount}곡 · ${playlistDuration.to_hh_colon_mm_colon_ss()}"
    val countString: String get() = if (audioMediaCount == 0) "비어 있음" else "${audioMediaCount}곡"

    companion object {
        fun getMock(id: Int = 0, type: PlaylistType = PlaylistType.USER): PlaylistItemUiState {
            return PlaylistItemUiState(
                id = id,
                imageFileFullPath = null,
                name = "새로운 플레이리스트",
                playlistDuration = 12000.seconds,
                audioMediaCount = 999,
                description = "",
                sequence = id,
                type = type,
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
    type = type,
)
