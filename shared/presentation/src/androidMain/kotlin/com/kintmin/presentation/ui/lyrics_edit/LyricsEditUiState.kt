package com.kintmin.presentation.ui.lyrics_edit

data class LyricsEditUiState(
    val title: String,
    val durationMs: Long,
    val rows: List<EditRow>,
    val isLoading: Boolean,
    val isSaving: Boolean,
    val isDirty: Boolean,
    // 번역/음차 변형 가사 파일이 존재하면 각 줄에 편집칸을 노출한다.
    val hasTranslation: Boolean = false,
    val hasTransliteration: Boolean = false,
) {
    data class EditRow(
        val id: Int,
        val timeMs: Long,
        val text: String,
        val translation: String = "",
        val transliteration: String = "",
        val isModified: Boolean,
    )

    companion object {
        fun getMock(): LyricsEditUiState = LyricsEditUiState(
            title = "Yesterday",
            durationMs = 4 * 60_000L + 30_000L,
            rows = listOf(
                EditRow(
                    id = 0,
                    timeMs = 1_000L,
                    text = "Yesterday",
                    translation = "어제",
                    transliteration = "예스터데이",
                    isModified = false,
                ),
                EditRow(
                    id = 1,
                    timeMs = 5_500L,
                    text = "All my troubles seemed so far away",
                    translation = "내 모든 고민이 아주 멀게 느껴졌죠",
                    transliteration = "올 마이 트러블스 심드 소 파 어웨이",
                    isModified = true,
                ),
            ),
            isLoading = false,
            isSaving = false,
            isDirty = false,
            hasTranslation = true,
            hasTransliteration = true,
        )
    }
}
