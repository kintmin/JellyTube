package com.kintmin.domain.karaoke.usecase

import com.kintmin.domain.karaoke.model.KaraokeSong
import com.kintmin.domain.karaoke.repository.KaraokeRepository

class SearchKaraokeUseCase(
    private val karaokeRepository: KaraokeRepository,
) {
    suspend operator fun invoke(title: String): Result<List<KaraokeSong>> =
        karaokeRepository.searchBySongTitle(title)
}
