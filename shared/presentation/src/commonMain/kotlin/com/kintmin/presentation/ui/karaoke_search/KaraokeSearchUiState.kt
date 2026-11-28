package com.kintmin.presentation.ui.karaoke_search

data class KaraokeSearchUiState(
    val query: String,
    val isLoading: Boolean,
    val results: List<KaraokeSearchItem>,
    val isConfirmingExternalSearch: Boolean = false,
    /** 검색을 한 번이라도 걸었는가. 걸지 않았는데 "결과 없음" 을 보이면 찾아보고 빈 것처럼 읽힌다. */
    val hasSearched: Boolean = false,
) {
    val isEmptyResult: Boolean get() = hasSearched && !isLoading && results.isEmpty()

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
