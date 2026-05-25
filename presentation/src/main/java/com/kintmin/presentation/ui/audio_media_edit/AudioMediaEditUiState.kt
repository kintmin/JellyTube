package com.kintmin.presentation.ui.audio_media_edit

import com.kintmin.domain.audio_track.model.PlaylistTrackAggregate
import com.kintmin.presentation.extension.to_hh_colon_mm_colon_ss
import java.time.format.DateTimeFormatter
import kotlin.time.Duration.Companion.seconds

data class AudioMediaEditUiState(
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
    )

    companion object {

        fun getMock() = AudioMediaEditUiState(
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

internal fun List<PlaylistTrackAggregate>.toAudioMediaEditUiState(): AudioMediaEditUiState {
    val audioMedia = first().audioMedia
    return AudioMediaEditUiState(
        audioMediaId = audioMedia.id,
        imageFileFullPath = audioMedia.imageFileFullPath,
        audioMediaName = audioMedia.name,
        artist = audioMedia.artist,
        playTime = (audioMedia.audioDuration ?: 0.seconds).to_hh_colon_mm_colon_ss(),
        audioMediaCreationTime = DateTimeFormatter.ofPattern("yyyy년 M월 d일 H시 m분 s.S초")
            .format(audioMedia.createdTime),
        source = audioMedia.source,
        audioMediaDescription = audioMedia.description,
        playlists = this.map { dataList ->
            AudioMediaEditUiState.Playlist(
                playlistId = dataList.playlist.id,
                playlistName = dataList.playlist.name,
            )
        }
    )
}