package com.kintmin.presentation.ui.lyrics_detail

data class LyricsDetailUiState(
    val trackName: String,
    val artistName: String,
    val plainLyrics: String,
    val isSynced: Boolean,
    val syncedLines: List<SyncedLine>,
    val isApplying: Boolean,
) {
    data class SyncedLine(
        val timeMs: Long,
        val text: String,
    )

    companion object {

        fun getMock() = LyricsDetailUiState(
            trackName = "밤편지",
            artistName = "아이유 (IU)",
            plainLyrics = "",
            isSynced = true,
            syncedLines = listOf(
                SyncedLine(1_000L, "이 밤 그날의 반딧불을"),
                SyncedLine(5_500L, "당신의 창 가까이 보낼게요"),
                SyncedLine(12_000L, "음 사랑한다는 말이에요"),
            ),
            isApplying = false,
        )
    }
}
