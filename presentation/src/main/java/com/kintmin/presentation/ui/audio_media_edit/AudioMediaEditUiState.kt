package com.kintmin.presentation.ui.audio_media_edit

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