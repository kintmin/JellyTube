package com.kintmin.presentation.ui.audio_media_information_edit

import com.kintmin.domain.audio_track.model.PlaylistTrackAggregate
import com.kintmin.presentation.extension.to_hh_colon_mm_colon_ss
import com.kintmin.presentation.extension.toKoreanDateTimeString
import kotlin.time.Duration.Companion.seconds

data class AudioMediaInformationEditUiState(
    val audioMediaId: Int,
    val imageFileFullPath: String?,
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
            audioMediaName = "",
            artist = "",
            playTime = "",
            audioMediaCreationTime = "",
            source = "",
            audioMediaDescription = "",
            playlists = List(3) {
                Playlist(
                    playlistId = it,
                    playlistName = "플레이리스트 $it",
                )
            },
        )
    }
}

internal fun List<PlaylistTrackAggregate>.toAudioMediaInformationEditUiState(): AudioMediaInformationEditUiState {
    val audioMedia = first().audioMedia
    return AudioMediaInformationEditUiState(
        audioMediaId = audioMedia.id,
        imageFileFullPath = audioMedia.imageFileFullPath,
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
