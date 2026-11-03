package com.kintmin.presentation.ui.karaoke_search

data class KaraokeSearchUiState(
    val query: String,
    val isLoading: Boolean,
    val results: List<KaraokeSearchItem>,
) {
    // 로딩이 끝났는데 결과가 없는 상태 (검색 결과 없음 문구 노출 조건)
    val isEmptyResult: Boolean get() = !isLoading && results.isEmpty()

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
