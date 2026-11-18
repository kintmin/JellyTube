package com.kintmin.data.network.data_source

import com.kintmin.data.network.model.KaraokeSongDto

interface KaraokeDataSource {
    suspend fun searchBySongTitle(title: String): Result<List<KaraokeSongDto>>
}
