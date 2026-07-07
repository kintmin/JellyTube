package com.kintmin.domain.lyrics.repository

import com.kintmin.domain.lyrics.model.LyricsSearchResult

interface LyricsRepository {
    suspend fun search(query: String): Result<List<LyricsSearchResult>>
}
