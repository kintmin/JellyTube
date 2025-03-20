package com.kintmin.domain.usecase

import com.kintmin.domain.repository.YoutubeMediaRepository
import javax.inject.Inject

class FetchYoutubeMediaUseCase @Inject constructor(
    private val youtubeMediaRepository: YoutubeMediaRepository,
) {
    suspend operator fun invoke(youtubeUrl: String) = runCatching {
        val videoId = extractVideoId(youtubeUrl) ?: throw Exception("유튜브 url 형식이 아닙니다.")

        youtubeMediaRepository.getMediaDataFromMetaData(videoId).getOrNull()
            ?: youtubeMediaRepository.getMediaData(youtubeUrl, videoId).onSuccess {
                youtubeMediaRepository.saveMetaData(it).onFailure { exception ->
                    youtubeMediaRepository.deleteCacheData(videoId)
                    throw exception
                }
            }.getOrThrow()
    }

    private fun extractVideoId(youtubeUrl: String): String? {
        val regex = Regex("v=([a-zA-Z0-9_-]{11})")
        return regex.find(youtubeUrl)?.groupValues?.get(1)
    }
}