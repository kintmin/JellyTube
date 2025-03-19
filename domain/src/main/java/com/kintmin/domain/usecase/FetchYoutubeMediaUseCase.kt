package com.kintmin.domain.usecase

import com.kintmin.domain.repository.YoutubeMediaRepository
import javax.inject.Inject

class FetchYoutubeMediaUseCase @Inject constructor(
    private val youtubeMediaRepository: YoutubeMediaRepository,
) {
    suspend operator fun invoke(youtubeUrl: String) = runCatching {
        val videoId = extractVideoId(youtubeUrl) ?: throw Exception("유튜브 url 형식이 아닙니다.")

        if (youtubeMediaRepository.isExistData(videoId).getOrDefault(false)) {
            if (youtubeMediaRepository.isExistFile(videoId).getOrDefault(false)) {
                return@runCatching youtubeMediaRepository.getCachedMediaData(videoId).getOrThrow()
            } else {
                youtubeMediaRepository.deleteData(videoId)
            }
        }

        youtubeMediaRepository.getMediaData(videoId).onSuccess {
            youtubeMediaRepository.downloadThumbnail(it.imageFilePath, it.videoId)
            youtubeMediaRepository.saveData(it)
        }.getOrThrow()
    }

    private fun extractVideoId(youtubeUrl: String): String? {
        val regex = Regex("v=([a-zA-Z0-9_-]{11})")
        return regex.find(youtubeUrl)?.groupValues?.get(1)
    }
}