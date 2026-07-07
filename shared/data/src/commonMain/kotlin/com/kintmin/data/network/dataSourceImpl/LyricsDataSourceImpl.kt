package com.kintmin.data.network.dataSourceImpl

import com.kintmin.data.network.dataSource.LyricsDataSource
import com.kintmin.data.network.model.LyricsSearchResultDto
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.get
import io.ktor.client.request.header
import io.ktor.client.request.parameter
import io.ktor.http.HttpHeaders

// LRCLIB 가사 검색 API 공식 문서: https://lrclib.net/docs
internal class LyricsDataSourceImpl(
    private val client: HttpClient,
) : LyricsDataSource {

    override suspend fun search(query: String): Result<List<LyricsSearchResultDto>> = runCatching {
        client.get(LRCLIB_SEARCH_URL) {
            parameter("q", query)
            header(HttpHeaders.UserAgent, USER_AGENT)
        }.body()
    }
}

private const val LRCLIB_SEARCH_URL = "https://lrclib.net/api/search"
private const val USER_AGENT = "JellyTube (https://github.com/kintmin/JellyTube)"
