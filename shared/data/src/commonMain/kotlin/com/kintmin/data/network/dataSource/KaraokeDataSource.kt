package com.kintmin.data.network.dataSource

import com.kintmin.data.network.model.KaraokeSongDto

interface KaraokeDataSource {
    suspend fun searchBySongTitle(title: String): Result<List<KaraokeSongDto>>
}
