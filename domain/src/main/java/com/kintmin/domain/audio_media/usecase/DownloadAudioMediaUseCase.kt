package com.kintmin.domain.audio_media.usecase

import com.kintmin.domain.audio_media.model.AudioMedia
import com.kintmin.domain.audio_media.repository.AudioMediaRepository
import com.kintmin.domain.audio_track.repository.AudioTrackRepository
import com.kintmin.domain.device.repository.DeviceStatusRepository
import com.kintmin.domain.playlist.model.Playlist
import com.kintmin.domain.playlist.usecase.internal.UpdateOnPlaylistChangeUseCase
import com.kintmin.log.FirebaseEvent
import com.kintmin.log.Log
import kotlinx.coroutines.joinAll
import kotlinx.coroutines.launch
import kotlinx.coroutines.supervisorScope
import javax.inject.Inject

class DownloadAudioMediaUseCase @Inject constructor(
    private val audioMediaRepository: AudioMediaRepository,
    private val audioTrackRepository: AudioTrackRepository,
    private val updateOnPlaylistChangeUseCase: UpdateOnPlaylistChangeUseCase,
    private val log: Log,
    private val deviceStatusRepository: DeviceStatusRepository,
) {
    suspend operator fun invoke(downloadUrl: String): Result<AudioMedia> = runCatching {
        // 다운한 목록 중 동일한 출처가 있다면 제외한다.
        audioMediaRepository.getAudioMediaBySource(source = downloadUrl).getOrElse {
            // 미디어 다운 및 추가를 하는 중 예외가 발생 시 그대로 throw한다.
            audioMediaRepository.addAudioMedia(downloadUrl).onSuccess { audioMedia ->
                supervisorScope {
                    listOf(
                        // 전체 추가는 무조건 보장해야 하므로 예외 발생 시 그대로 throw한다.
                        launch {
                            val sequence = audioTrackRepository.addAudioTrack(Playlist.TOTAL, audioMedia.id).getOrThrow()
                            log.sendFirebaseEvent(FirebaseEvent.AddAudioMedia(downloadUrl, sequence))
                        },
                        // 미분류 추가는 오류가 나도 무시한다.
                        launch { audioTrackRepository.addAudioTrack(Playlist.UNCATEGORIZED, audioMedia.id) },
                    ).joinAll()

                    // 업데이트는 오류가 나도 무시한다.
                    listOf(
                        launch { updateOnPlaylistChangeUseCase(Playlist.TOTAL) },
                        launch { updateOnPlaylistChangeUseCase(Playlist.UNCATEGORIZED) },
                    ).joinAll()


                }
            }.getOrThrow()
        }
    }.onFailure { exception ->
        val systemMemory = deviceStatusRepository.getSystemMemory().getOrNull()
        val connectionStatus = deviceStatusRepository.getConnectionStatus().getOrNull()

        log.sendFirebaseEvent(
            FirebaseEvent.FailedDownloadAudioMedia(
                source = downloadUrl,
                exception = exception,
                availableRemMemory = systemMemory?.availableRemMemory,
                isLowRemMemory = systemMemory?.isLowRemMemory,
                availableStorage = systemMemory?.availableStorage,
                isConnected = connectionStatus?.isConnected,
                isWifi = connectionStatus?.isWifi,
                isCellular = connectionStatus?.isCellular,
                downstreamKbps = connectionStatus?.downstreamKbps,
                upstreamKbps = connectionStatus?.upstreamKbps,
            )
        )
    }
}
