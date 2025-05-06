package com.kintmin.domain.usecase

import com.kintmin.domain.internal_usecase.UpdatePlaylistAfterUpdatePlaybackUseCase
import com.kintmin.domain.model.AudioMedia
import com.kintmin.domain.model.Playlist
import com.kintmin.domain.platform_api.Log
import com.kintmin.domain.repository.AudioMediaRepository
import javax.inject.Inject

class DownloadYoutubeMediaUseCase @Inject constructor(
    private val log: Log,
    private val audioMediaRepository: AudioMediaRepository,
    private val updatePlaylistAfterUpdatePlaybackUseCase: UpdatePlaylistAfterUpdatePlaybackUseCase,
) {
    suspend operator fun invoke(youtubeUrl: String): Result<AudioMedia> = runCatching {
        audioMediaRepository.getAudioMediaBySource(source = youtubeUrl).onFailure {
            log.d("DownloadYoutubeMediaUseCase", "로컬 음원 가져오기 실패 - ${it.message}: ${it.cause}")
        }.getOrElse {
            val downloadData = audioMediaRepository.downloadAudioMedia(youtubeUrl).onFailure { exception ->
                log.d("DownloadYoutubeMediaUseCase", "음원 다운로드 실패 - ${exception.message}: ${exception.cause}")
            }.getOrThrow()

            log.d("DownloadYoutubeMediaUseCase", "음원 다운로드 성공 - $downloadData")

            audioMediaRepository.addAudioMedia(downloadData).onSuccess {
                updatePlaylistAfterUpdatePlaybackUseCase(Playlist.TOTAL)
                updatePlaylistAfterUpdatePlaybackUseCase(Playlist.UNCATEGORIZED)
            }.onFailure { exception ->
                log.d("DownloadYoutubeMediaUseCase", "음원 저장 실패 - ${exception.message}: ${exception.cause}")
            }.getOrThrow()
        }
    }
}