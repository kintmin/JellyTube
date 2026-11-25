package com.kintmin.presentation.ui.audio_media_information_edit

import com.kintmin.domain.audio_track.model.PlaylistTrackAggregate
import com.kintmin.presentation.extension.to_hh_colon_mm_colon_ss
import com.kintmin.presentation.extension.toKoreanDateTimeString
import kotlin.time.Duration.Companion.seconds

data class AudioMediaInformationEditUiState(
    val audioMediaId: Int,
    val imageFileFullPath: String?,
    /** 사용자가 고른 썸네일인지. 받아 온 정본으로 되돌릴 수 있는지가 여기서 갈린다. */
    val isCustomImage: Boolean = false,
    val audioMediaName: String,
    val artist: String,
    val playTime: String,
    val audioMediaCreationTime: String,
    val source: String,
    val audioMediaDescription: String,
    val playlists: List<Playlist>,
    val selectablePlaylists: List<Playlist> = emptyList(),
    val isAddPlaylistBottomSheetVisible: Boolean = false,
) {

    data class Playlist(
        val playlistId: Int,
        val playlistName: String,
        val isBasePlaylist: Boolean = false,
    )

    companion object {

        fun getMock() = AudioMediaInformationEditUiState(
            audioMediaId = 1,
            imageFileFullPath = null,
            audioMediaName = "밤양갱",
            artist = "비비 (BIBI)",
            playTime = "00:02:26",
            audioMediaCreationTime = "2026년 11월 20일 14시 30분",
            source = "https://www.youtube.com/watch?v=dQw4w9WgXcQ",
            audioMediaDescription = "",
            playlists = listOf(
                Playlist(playlistId = 1, playlistName = "미분류", isBasePlaylist = true),
                Playlist(playlistId = 2, playlistName = "운동할 때 듣는 음악"),
                Playlist(playlistId = 3, playlistName = "출퇴근 플레이리스트"),
            ),
        )
    }
}

internal fun List<PlaylistTrackAggregate>.toAudioMediaInformationEditUiState(): AudioMediaInformationEditUiState {
    val audioMedia = first().audioMedia
    return AudioMediaInformationEditUiState(
        audioMediaId = audioMedia.id,
        imageFileFullPath = audioMedia.displayImageFileFullPath,
        isCustomImage = audioMedia.customImageFileFullPath != null,
        audioMediaName = audioMedia.name,
        artist = audioMedia.artist,
        playTime = (audioMedia.playbackDuration ?: 0.seconds).to_hh_colon_mm_colon_ss(),
        audioMediaCreationTime = audioMedia.createdTime.toKoreanDateTimeString(),
        source = audioMedia.source,
        audioMediaDescription = audioMedia.description,
        playlists = this.map { dataList ->
            AudioMediaInformationEditUiState.Playlist(
                playlistId = dataList.playlist.id,
                playlistName = dataList.playlist.name,
                isBasePlaylist = dataList.playlist.isBasePlaylist,
            )
        }
    )
}
