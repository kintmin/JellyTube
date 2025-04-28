package com.kintmin.domain.usecase

import com.kintmin.domain.platform_api.Log
import com.kintmin.domain.repository.AudioMediaRepository
import javax.inject.Inject

class DownloadYoutubeMediaUseCase @Inject constructor(
    private val log: Log,
    private val audioMediaRepository: AudioMediaRepository,
    private val extractYoutubeVideoIdUseCase: ExtractYoutubeVideoIdUseCase,
) {
    suspend operator fun invoke(youtubeUrl: String): Result<Unit> = runCatching {
        val videoId = extractYoutubeVideoIdUseCase(youtubeUrl)

        val isExistAudioMedia = audioMediaRepository.isExistAudioMedia(videoId).onFailure {
            log.d("DownloadYoutubeMediaUseCase", "로컬 음원 가져오기 실패 - ${it.message}: ${it.cause}")
        }.getOrThrow()
        if (isExistAudioMedia) return@runCatching

        audioMediaRepository.deleteInvalidAudioMediaFile(videoId)

        audioMediaRepository.downloadAudioMedia(youtubeUrl, videoId).onSuccess { downloadData ->
            log.d("DownloadYoutubeMediaUseCase", "음원 다운로드 성공 - $downloadData")
            audioMediaRepository.addAudioMedia(downloadData).onFailure { exception ->
                log.d("DownloadYoutubeMediaUseCase", "음원 저장 실패 - ${exception.message}: ${exception.cause}")
                throw exception
            }.getOrThrow()
        }.onFailure { exception ->
            log.d("DownloadYoutubeMediaUseCase", "음원 다운로드 실패 - ${exception.message}: ${exception.cause}")
            throw exception
        }.getOrThrow()
    }
}