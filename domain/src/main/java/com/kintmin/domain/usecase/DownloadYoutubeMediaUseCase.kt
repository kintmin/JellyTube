package com.kintmin.domain.usecase

import com.kintmin.domain.internal_usecase.UpdatePlaylistCountAndPlayTimeWhenUpdatePlaybackUseCase
import com.kintmin.domain.internal_usecase.UpdatePlaylistImageWhenUpdatePlaybackUseCase
import com.kintmin.domain.model.AudioMedia
import com.kintmin.domain.model.Playlist
import com.kintmin.domain.platform_api.Log
import com.kintmin.domain.repository.AudioMediaRepository
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import javax.inject.Inject

class DownloadYoutubeMediaUseCase @Inject constructor(
    private val log: Log,
    private val audioMediaRepository: AudioMediaRepository,
    private val updatePlaylistCountAndPlayTimeWhenUpdatePlaybackUseCase: UpdatePlaylistCountAndPlayTimeWhenUpdatePlaybackUseCase,
    private val updatePlaylistImageWhenUpdatePlaybackUseCase: UpdatePlaylistImageWhenUpdatePlaybackUseCase,
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
                coroutineScope {
                    listOf(
                        async { updatePlaylistCountAndPlayTimeWhenUpdatePlaybackUseCase(Playlist.TOTAL) },
                        async { updatePlaylistImageWhenUpdatePlaybackUseCase(Playlist.TOTAL) },
                        async { updatePlaylistCountAndPlayTimeWhenUpdatePlaybackUseCase(Playlist.UNCATEGORIZED) },
                        async { updatePlaylistImageWhenUpdatePlaybackUseCase(Playlist.UNCATEGORIZED) },
                    ).awaitAll()
                }
            }.onFailure { exception ->
                log.d("DownloadYoutubeMediaUseCase", "음원 저장 실패 - ${exception.message}: ${exception.cause}")
            }.getOrThrow()
        }
    }
}