package com.kintmin.presentation.ui.playlist_edit.header

import com.kintmin.domain.playlist.model.Playlist
import com.kintmin.presentation.extension.to_hh_colon_mm_colon_ss
import kotlin.time.Duration.Companion.seconds

data class PlaylistEditHeaderUiState(
    val id: Int,
    val imageFileFullPath: String?,
    val isCustomImage: Boolean = false,
    val name: String,
    val description: String,
    val audioMediaCount: Int,
    val playTimeDuration: String,
    val isBasePlaylist: Boolean = false,
    /** 곡을 뺄 때 어디로 가는지 달라진다 — 비밀에서 빠지면 드러나는 곳으로 간다. */
    val isSecretPlaylist: Boolean = false,
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
    isCustomImage = isCustomImage,
    name = name,
    description = description,
    audioMediaCount = audioMediaCount,
    playTimeDuration = playTimeDuration.to_hh_colon_mm_colon_ss(),
    isBasePlaylist = isBasePlaylist,
    isSecretPlaylist = type.isSecret,
)