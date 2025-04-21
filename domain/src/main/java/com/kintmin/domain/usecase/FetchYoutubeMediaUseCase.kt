package com.kintmin.domain.usecase

import com.kintmin.domain.model.AudioMedia
import com.kintmin.domain.platform_api.Log
import com.kintmin.domain.repository.AudioMediaRepository
import javax.inject.Inject

class FetchYoutubeMediaUseCase @Inject constructor(
    private val log: Log,
    private val audioMediaRepository: AudioMediaRepository,
    private val extractYoutubeVideoIdUseCase: ExtractYoutubeVideoIdUseCase,
) {
    suspend operator fun invoke(youtubeUrl: String): Result<AudioMedia> = runCatching {
        val videoId = extractYoutubeVideoIdUseCase(youtubeUrl)
        audioMediaRepository.getAudioMedia(videoId).onFailure {
            log.d("DEBUG", "로컬 음원 가져오기 실패 - ${it.message}: ${it.cause}")
        }.getOrElse {
            audioMediaRepository.deleteAudioMediaInvalidCache(videoId)
            audioMediaRepository.downloadAudioMedia(youtubeUrl, videoId).onSuccess { downloadData ->
                log.d("DEBUG", "음원 다운로드 성공 - $downloadData")
                audioMediaRepository.addAudioMedia(downloadData).onFailure { exception ->
                    log.d("DEBUG", "음원 저장 실패 - ${exception.message}: ${exception.cause}")
                    throw exception
                }.getOrThrow()
            }.onFailure { exception ->
                log.d("DEBUG", "음원 다운로드 실패 - ${exception.message}: ${exception.cause}")
                throw exception
            }.getOrThrow()
        }
    }
}