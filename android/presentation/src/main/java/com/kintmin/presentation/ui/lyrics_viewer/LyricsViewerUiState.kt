package com.kintmin.presentation.ui.lyrics_viewer

data class LyricsViewerUiState(
    val title: String,
    val lines: List<String>,
    val activeIndex: Int,
    val isSynced: Boolean,
    val isLoading: Boolean,
    // 번역/음차 변형 가사 (원본 lines 와 인덱스 정렬, 없으면 null)
    val translationLines: List<String>? = null,
    val transliterationLines: List<String>? = null,
    // ⋮ 메뉴 노출 여부
    val showTranslateMenu: Boolean = false,
    val showTransliterateMenu: Boolean = false,
) {
    companion object {

        fun getMock() = LyricsViewerUiState(
            title = "Yesterday",
            lines = listOf(
                "Yesterday",
                "All my troubles seemed so far away",
            ),
            activeIndex = 1,
            isSynced = true,
            isLoading = false,
            transliterationLines = listOf(
                "예스터데이",
                "올 마이 트러블스 심드 소 파 어웨이",
            ),
            translationLines = listOf(
                "어제",
                "내 모든 고민이 아주 멀게 느껴졌죠",
            ),
            showTranslateMenu = true,
            showTransliterateMenu = false,
        )
    }
}
