package com.kintmin.domain.lyrics.usecase

import com.kintmin.domain.lyrics.model.LyricsSearchResult
import com.kintmin.domain.lyrics.repository.LyricsRepository

class SearchLyricsUseCase(
    private val lyricsRepository: LyricsRepository,
) {
    suspend operator fun invoke(query: String): Result<List<LyricsSearchResult>> =
        lyricsRepository.search(query)
}
