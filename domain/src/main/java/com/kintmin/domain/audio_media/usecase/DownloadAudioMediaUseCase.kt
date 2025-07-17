package com.kintmin.domain.audio_media.usecase

import com.kintmin.domain.audio_media.model.AudioMedia
import com.kintmin.domain.audio_media.repository.AudioMediaRepository
import com.kintmin.domain.audio_track.repository.AudioTrackRepository
import com.kintmin.domain.common.platform_api.Log
import com.kintmin.domain.playlist.model.Playlist
import com.kintmin.domain.playlist.usecase.UpdatePlaylistCountAndPlayTimeWhenUpdatePlaybackUseCase
import com.kintmin.domain.playlist.usecase.UpdatePlaylistImageWhenUpdateTrackUseCase
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.withContext
import javax.inject.Inject

class DownloadAudioMediaUseCase @Inject constructor(
    private val audioMediaRepository: AudioMediaRepository,
    private val audioTrackRepository: AudioTrackRepository,
    private val updatePlaylistCountAndPlayTimeWhenUpdatePlaybackUseCase: UpdatePlaylistCountAndPlayTimeWhenUpdatePlaybackUseCase,
    private val updatePlaylistImageWhenUpdateTrackUseCase: UpdatePlaylistImageWhenUpdateTrackUseCase,
) {
    suspend operator fun invoke(downloadUrl: String): Result<AudioMedia> = runCatching {
        audioMediaRepository.getAudioMediaBySource(source = downloadUrl).onFailure {
//            log.d("DownloadAudioMediaUseCase", "로컬 음원 가져오기 실패 - ${it.message}: ${it.cause}")
        }.getOrElse {
            audioMediaRepository.addAudioMedia(downloadUrl).onSuccess { audioMedia ->
                withContext(Dispatchers.IO) {
                    runCatching {
                        listOf(
                            async { audioTrackRepository.addAudioTrack(Playlist.TOTAL, audioMedia.id).getOrThrow() },
                            async { audioTrackRepository.addAudioTrack(Playlist.UNCATEGORIZED, audioMedia.id).getOrThrow() },
                        ).awaitAll()
                    }.onFailure {
//                        log.d("DownloadAudioMediaUseCase", "DB 추가 실패")
                    }.getOrThrow()

                    runCatching {
                        listOf(
                            async { updatePlaylistCountAndPlayTimeWhenUpdatePlaybackUseCase(Playlist.TOTAL) },
                            async { updatePlaylistImageWhenUpdateTrackUseCase(Playlist.TOTAL) },
                            async { updatePlaylistCountAndPlayTimeWhenUpdatePlaybackUseCase(Playlist.UNCATEGORIZED) },
                            async { updatePlaylistImageWhenUpdateTrackUseCase(Playlist.UNCATEGORIZED) },
                        ).awaitAll()
                    }.onFailure {
//                        log.d("DownloadAudioMediaUseCase", "DB 업데이트 실패")
                    }.getOrThrow()
                }
            }.onFailure { exception ->
//                log.d("DownloadAudioMediaUseCase", "음원 저장 실패 - ${exception.message}: ${exception.cause}")
            }.getOrThrow()
        }
    }
}