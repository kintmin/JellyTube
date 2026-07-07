package com.kintmin.presentation.ui.lyrics_detail

data class LyricsDetailUiState(
    val trackName: String,
    val artistName: String,
    val plainLyrics: String,
    val syncedLyrics: String,
    val isApplying: Boolean,
) {
    // 화면에는 일반 가사를 우선 표시하고, 없으면 싱크 가사 원문을 표시한다.
    val displayLyrics: String get() = plainLyrics.ifBlank { syncedLyrics }

    companion object {

        fun getMock() = LyricsDetailUiState(
            trackName = "밤편지",
            artistName = "아이유 (IU)",
            plainLyrics = "이 밤 그날의 반딧불을\n당신의 창 가까이 보낼게요\n음 사랑한다는 말이에요",
            syncedLyrics = "",
            isApplying = false,
        )
    }
}
