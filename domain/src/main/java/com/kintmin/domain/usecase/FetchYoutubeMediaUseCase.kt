package com.kintmin.domain.usecase

import com.kintmin.domain.model.AudioMediaData
import com.kintmin.domain.repository.AudioMediaRepository
import javax.inject.Inject

class FetchYoutubeMediaUseCase @Inject constructor(
    private val audioMediaRepository: AudioMediaRepository,
    private val extractYoutubeVideoIdUseCase: ExtractYoutubeVideoIdUseCase,
) {
    suspend operator fun invoke(youtubeUrl: String): Result<AudioMediaData> = runCatching {
        val videoId = extractYoutubeVideoIdUseCase(youtubeUrl)
        audioMediaRepository.getLocalData(videoId).getOrNull() ?: audioMediaRepository.downloadData(
            youtubeUrl,
            videoId,
        ).onSuccess { downloadData ->
            audioMediaRepository.saveDataToLocal(downloadData).onFailure {
                audioMediaRepository.deleteMediaFile(videoId)
            }
        }.getOrThrow()
    }
}