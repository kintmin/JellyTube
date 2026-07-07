package com.kintmin.data.network.dataSource

import com.kintmin.data.network.model.LyricsSearchResultDto

interface LyricsDataSource {
    suspend fun search(query: String): Result<List<LyricsSearchResultDto>>
}
