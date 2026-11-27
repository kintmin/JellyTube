package com.kintmin.presentation.ui.karaoke_search

data class KaraokeSearchUiState(
    val query: String,
    val isLoading: Boolean,
    val results: List<KaraokeSearchItem>,
    val isConfirmingExternalSearch: Boolean = false,
) {
    // 아직 묻는 중이면 검색을 건 적이 없다. "결과 없음" 을 뒤에 깔면 찾아보고 빈 것처럼 읽힌다.
    val isEmptyResult: Boolean get() = !isLoading && !isConfirmingExternalSearch && results.isEmpty()

    data class KaraokeSearchItem(
        val id: Int,
        val number: String,
        val title: String,
        val singer: String,
    )

    companion object {

        fun getMock() = KaraokeSearchUiState(
            query = "벚꽃엔딩",
            isLoading = false,
            results = List(3) { index ->
                KaraokeSearchItem(
                    id = index,
                    number = "3808${index}",
                    title = "벚꽃엔딩",
                    singer = "버스커버스커",
                )
            },
        )
    }
}
