package com.kintmin.presentation.ui.lyrics_search

data class LyricsSearchUiState(
    val audioMediaName: String,
    val playbackDurationText: String,
    val query: String,
    val isLoading: Boolean,
    val results: List<LyricsSearchItem>,
) {
    val isEmptyResult: Boolean get() = !isLoading && results.isEmpty()

    data class LyricsSearchItem(
        val id: Int,
        val trackName: String,
        val artistName: String,
        val albumName: String,
        val durationText: String,
        val plainLyricsPreview: String,
        val plainLyrics: String,
        val syncedLyrics: String,
    )

    companion object {

        fun getMock() = LyricsSearchUiState(
            audioMediaName = "밤편지",
            playbackDurationText = "00:03:34",
            query = "아이유 밤편지",
            isLoading = false,
            results = List(3) { index ->
                LyricsSearchItem(
                    id = index,
                    trackName = "밤편지",
                    artistName = "아이유 (IU)",
                    albumName = "Palette",
                    durationText = "00:03:34",
                    plainLyricsPreview = "이 밤 그날의 반딧불을 당신의 창 가까이 보낼게요",
                    plainLyrics = "이 밤 그날의 반딧불을\n당신의 창 가까이 보낼게요",
                    syncedLyrics = "",
                )
            },
        )
    }
}
