package com.kintmin.presentation.ui.lyrics_viewer

data class LyricsViewerUiState(
    val title: String,
    val lines: List<String>,
    val activeIndex: Int,
    val isSynced: Boolean,
    val isLoading: Boolean,
) {
    companion object {

        fun getMock() = LyricsViewerUiState(
            title = "밤편지",
            lines = listOf(
                "이 밤 그날의 반딧불을",
                "당신의 창 가까이 보낼게요",
                "음 사랑한다는 말이에요",
                "나 우리의 첫 봄을 기억해요",
            ),
            activeIndex = 1,
            isSynced = true,
            isLoading = false,
        )
    }
}
