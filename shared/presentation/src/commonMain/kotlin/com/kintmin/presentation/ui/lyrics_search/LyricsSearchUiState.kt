package com.kintmin.presentation.ui.lyrics_search

data class LyricsSearchUiState(
    val audioMediaName: String,
    val playbackDurationText: String,
    val query: String,
    val isLoading: Boolean,
    val results: List<LyricsSearchItem>,
    val isConfirmingExternalSearch: Boolean = false,
    /** 검색을 한 번이라도 걸었는가. 걸지 않았는데 "결과 없음" 을 보이면 찾아보고 빈 것처럼 읽힌다. */
    val hasSearched: Boolean = false,
) {
    val isEmptyResult: Boolean get() = hasSearched && !isLoading && results.isEmpty()

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
            playbackDurationText = "3:34",
            query = "아이유 밤편지",
            isLoading = false,
            results = List(3) { index ->
                LyricsSearchItem(
                    id = index,
                    trackName = "밤편지",
                    artistName = "아이유 (IU)",
                    albumName = "Palette",
                    durationText = "3:34",
                    plainLyricsPreview = "이 밤 그날의 반딧불을 당신의 창 가까이 보낼게요",
                    plainLyrics = "이 밤 그날의 반딧불을\n당신의 창 가까이 보낼게요",
                    syncedLyrics = "",
                )
            },
        )
    }
}
