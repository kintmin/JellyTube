package com.kintmin.domain.karaoke.repository

import com.kintmin.domain.karaoke.model.KaraokeSong

interface KaraokeRepository {
    suspend fun searchBySongTitle(title: String): Result<List<KaraokeSong>>
}
