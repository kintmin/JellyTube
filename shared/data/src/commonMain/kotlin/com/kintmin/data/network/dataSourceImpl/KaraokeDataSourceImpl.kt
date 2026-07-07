package com.kintmin.data.network.dataSourceImpl

import com.kintmin.data.network.dataSource.KaraokeDataSource
import com.kintmin.data.network.model.KaraokeSongDto
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.get
import io.ktor.client.request.parameter
import io.ktor.http.appendPathSegments
import io.ktor.http.takeFrom

// Manana 노래방 오픈 API(비공식): https://api.manana.kr/karaoke
internal class KaraokeDataSourceImpl(
    private val client: HttpClient,
) : KaraokeDataSource {

    override suspend fun searchBySongTitle(title: String): Result<List<KaraokeSongDto>> = runCatching {
        client.get {
            url {
                takeFrom(MANANA_BASE_URL)
                appendPathSegments("karaoke", "song", "$title.json")
            }
            parameter("brand", BRAND_TJ)
        }.body()
    }
}

private const val MANANA_BASE_URL = "https://api.manana.kr"
private const val BRAND_TJ = "tj"
