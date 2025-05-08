package com.kintmin.presentation.ui.audio_media_detail

data class AudioMediaDetailUiState(
    val audioMediaId: Int,
    val audioMediaName: String,
    val artist: String,
    val playTime: String,
    val audioMediaCreationTime: String,
    val source: String,
    val playlistId: Int,
    val playlistName: String,
    val playlistCreationTime: String,
    val playlistAddedTime: String,
    val audioMediaDescription: String,
) {

    companion object {

        fun getMock(index: Int = 0) = AudioMediaDetailUiState(
            audioMediaId = index,
            audioMediaName = "새로운 미디어",
            artist = "아티스트",
            playTime = "999:99",
            audioMediaCreationTime = "yyyy년 M월 dd일 HH:mm",
            source = "https://preview.com",
            playlistId = index,
            playlistName = "새로운 플레이리스트",
            playlistCreationTime = "yyyy년 M월 dd일 HH:mm",
            playlistAddedTime = "yyyy년 M월 dd일 HH:mm",
            audioMediaDescription = "이것은 최대 100자까지 가능한 미디어 설명입니다. 이것은 최대 100자까지 가능한 미디어 설명입니다. 이것은 최대 100자까지 가능한 미디어 설명입니다.",
        )
    }
}
