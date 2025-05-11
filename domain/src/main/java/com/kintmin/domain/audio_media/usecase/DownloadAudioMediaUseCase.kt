package com.kintmin.domain.audio_media.usecase

import com.kintmin.domain.audio_media.model.AudioMedia
import com.kintmin.domain.audio_media.repository.AudioMediaRepository
import com.kintmin.domain.common.platform_api.Log
import com.kintmin.domain.playlist.model.Playlist
import com.kintmin.domain.playlist.usecase.UpdatePlaylistCountAndPlayTimeWhenUpdatePlaybackUseCase
import com.kintmin.domain.playlist.usecase.UpdatePlaylistImageWhenUpdateTrackUseCase
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import javax.inject.Inject

class DownloadAudioMediaUseCase @Inject constructor(
    private val log: Log,
    private val audioMediaRepository: AudioMediaRepository,
    private val updatePlaylistCountAndPlayTimeWhenUpdatePlaybackUseCase: UpdatePlaylistCountAndPlayTimeWhenUpdatePlaybackUseCase,
    private val updatePlaylistImageWhenUpdateTrackUseCase: UpdatePlaylistImageWhenUpdateTrackUseCase,
) {
    suspend operator fun invoke(downloadUrl: String): Result<AudioMedia> = runCatching {
        audioMediaRepository.getAudioMediaBySource(source = downloadUrl).onFailure {
            log.d("DownloadAudioMediaUseCase", "로컬 음원 가져오기 실패 - ${it.message}: ${it.cause}")
        }.getOrElse {
            audioMediaRepository.addAudioMedia(downloadUrl).onSuccess {
                coroutineScope {
                    listOf(
                        async { updatePlaylistCountAndPlayTimeWhenUpdatePlaybackUseCase(Playlist.TOTAL) },
                        async { updatePlaylistImageWhenUpdateTrackUseCase(Playlist.TOTAL) },
                        async { updatePlaylistCountAndPlayTimeWhenUpdatePlaybackUseCase(Playlist.UNCATEGORIZED) },
                        async { updatePlaylistImageWhenUpdateTrackUseCase(Playlist.UNCATEGORIZED) },
                    ).awaitAll()
                }
            }.onFailure { exception ->
                log.d("DownloadAudioMediaUseCase", "음원 저장 실패 - ${exception.message}: ${exception.cause}")
            }.getOrThrow()
        }
    }
}