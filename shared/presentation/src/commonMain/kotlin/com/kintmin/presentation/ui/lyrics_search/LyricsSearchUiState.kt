package com.kintmin.presentation.ui.lyrics_search

data class LyricsSearchUiState(
    val audioMediaName: String,
    val playbackDurationText: String,
    val query: String,
    val isLoading: Boolean,
    val results: List<LyricsSearchItem>,
    val isConfirmingExternalSearch: Boolean = false,
) {
    // 아직 묻는 중이면 검색을 건 적이 없다. "결과 없음" 을 뒤에 깔면 찾아보고 빈 것처럼 읽힌다.
    val isEmptyResult: Boolean get() = !isLoading && !isConfirmingExternalSearch && results.isEmpty()

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
