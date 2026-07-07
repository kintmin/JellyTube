package com.kintmin.presentation.ui.lyrics_edit

data class LyricsEditUiState(
    val title: String,
    val durationMs: Long,
    val rows: List<EditRow>,
    val isLoading: Boolean,
    val isSaving: Boolean,
    val isDirty: Boolean,
) {
    data class EditRow(
        val id: Int,
        val timeMs: Long,
        val text: String,
        val isModified: Boolean,
    )

    companion object {
        fun getMock(): LyricsEditUiState = LyricsEditUiState(
            title = "밤편지",
            durationMs = 4 * 60_000L + 30_000L,
            rows = listOf(
                EditRow(id = 0, timeMs = 1_000L, text = "이 밤 그날의 반딧불을", isModified = false),
                EditRow(id = 1, timeMs = 5_500L, text = "당신의 창 가까이 보낼게요", isModified = true),
                EditRow(id = 2, timeMs = 11_000L, text = "음 사랑한다는 말이에요", isModified = false),
            ),
            isLoading = false,
            isSaving = false,
            isDirty = false,
        )
    }
}
