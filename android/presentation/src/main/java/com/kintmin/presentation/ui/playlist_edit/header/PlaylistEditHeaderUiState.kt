package com.kintmin.presentation.ui.playlist_edit.header

import com.kintmin.domain.playlist.model.Playlist
import com.kintmin.presentation.extension.to_hh_colon_mm_colon_ss
import kotlin.time.Duration.Companion.seconds

data class PlaylistEditHeaderUiState(
    val id: Int,
    val imageFileFullPath: String?,
    val name: String,
    val description: String,
    val audioMediaCount: Int,
    val playTimeDuration: String,
) {
    companion object {
        fun getMock() = PlaylistEditHeaderUiState(
            id = 1,
            imageFileFullPath = null,
            name = "새로운 플레이리스트",
            description = "설명은 최대 100자 설명은 최대 100자 설명은 최대 100자 설명은 최대 100자 ",
            audioMediaCount = 999,
            playTimeDuration = 9999.seconds.to_hh_colon_mm_colon_ss(),
        )
    }
}

internal fun Playlist.toPlaylistEditHeaderUiState() = PlaylistEditHeaderUiState(
    id = id,
    imageFileFullPath = imageFileFullPath,
    name = name,
    description = description,
    audioMediaCount = audioMediaCount,
    playTimeDuration = playTimeDuration.to_hh_colon_mm_colon_ss(),
)