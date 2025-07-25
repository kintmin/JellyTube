package com.kintmin.domain.audio_media.usecase

import com.kintmin.domain.audio_media.model.AudioMedia
import com.kintmin.domain.audio_media.repository.AudioMediaRepository
import com.kintmin.domain.audio_track.repository.AudioTrackRepository
import com.kintmin.domain.playlist.model.Playlist
import com.kintmin.domain.playlist.usecase.UpdatePlaylistCountAndPlayTimeWhenUpdatePlaybackUseCase
import com.kintmin.domain.playlist.usecase.UpdatePlaylistImageWhenUpdateTrackUseCase
import com.kintmin.log.FirebaseEvent
import com.kintmin.log.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.joinAll
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import javax.inject.Inject

class DownloadAudioMediaUseCase @Inject constructor(
    private val audioMediaRepository: AudioMediaRepository,
    private val audioTrackRepository: AudioTrackRepository,
    private val updatePlaylistCountAndPlayTimeWhenUpdatePlaybackUseCase: UpdatePlaylistCountAndPlayTimeWhenUpdatePlaybackUseCase,
    private val updatePlaylistImageWhenUpdateTrackUseCase: UpdatePlaylistImageWhenUpdateTrackUseCase,
    private val log: Log,
) {
    suspend operator fun invoke(downloadUrl: String): Result<AudioMedia> = runCatching {
        audioMediaRepository.getAudioMediaBySource(source = downloadUrl).getOrElse {
            audioMediaRepository.addAudioMedia(downloadUrl).onSuccess { audioMedia ->
                withContext(Dispatchers.IO) {
                    listOf(
                        launch { audioTrackRepository.addAudioTrack(Playlist.TOTAL, audioMedia.id).getOrThrow() },
                        launch { audioTrackRepository.addAudioTrack(Playlist.UNCATEGORIZED, audioMedia.id).getOrThrow() },
                    ).joinAll()

                    listOf(
                        launch { updatePlaylistCountAndPlayTimeWhenUpdatePlaybackUseCase(Playlist.TOTAL) },
                        launch { updatePlaylistImageWhenUpdateTrackUseCase(Playlist.TOTAL) },
                        launch { updatePlaylistCountAndPlayTimeWhenUpdatePlaybackUseCase(Playlist.UNCATEGORIZED) },
                        launch { updatePlaylistImageWhenUpdateTrackUseCase(Playlist.UNCATEGORIZED) },
                    ).joinAll()
                }
            }.onFailure {

                // 기기 메모리
                log.sendFirebaseEvent(FirebaseEvent.FailedDownloadAudioMedia(
                    url = downloadUrl,

                ))
            }.getOrThrow()
        }
    }
}