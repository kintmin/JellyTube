package com.kintmin.domain.audio_media.usecase

import com.kintmin.domain.audio_media.model.AudioMedia
import com.kintmin.domain.audio_media.repository.AudioMediaRepository
import com.kintmin.domain.device.repository.DeviceStatusRepository
import com.kintmin.log.FirebaseEvent
import com.kintmin.log.Log
import com.kintmin.log.LogcatEvent
import javax.inject.Inject
import kotlin.system.measureTimeMillis
import kotlin.time.measureTime
import kotlin.time.measureTimedValue

class DownloadAudioMediaUseCase @Inject constructor(
    private val audioMediaRepository: AudioMediaRepository,
    private val deviceStatusRepository: DeviceStatusRepository,
    private val log: Log,
) {
    private var downloadAttemptUrlList = mutableSetOf<String>()

    suspend operator fun invoke(downloadUrl: String): Result<AudioMedia> = runCatching {
        if (downloadAttemptUrlList.contains(downloadUrl)) {
            throw AlreadyDownloadingMedia()
        }
        downloadAttemptUrlList += downloadUrl

        audioMediaRepository.getAudioMediaBySource(source = downloadUrl).onSuccess {
            throw AlreadyDownloadedMedia()
        }

        val downloadedAudioMediaTimedValue = measureTimedValue {
            audioMediaRepository.downloadAudioMedia(downloadUrl).getOrThrow()
        }
        log.sendLogcatEvent(LogcatEvent("DownloadAudioMediaUseCase", "downloadAudioMedia: ${downloadedAudioMediaTimedValue.duration}"))

        val downloadedAudioMedia = downloadedAudioMediaTimedValue.value

        val resultTimedValue = measureTimedValue {
            val (audioMedia, totalPlaylistMediaCount) = audioMediaRepository.addAudioMedia(downloadedAudioMedia).onFailure {
                audioMediaRepository.deleteDownloadedFile(downloadedAudioMedia)
            }.getOrThrow()

            log.sendFirebaseEvent(FirebaseEvent.AddAudioMedia(downloadUrl, totalPlaylistMediaCount))
            audioMedia
        }
        log.sendLogcatEvent(LogcatEvent("DownloadAudioMediaUseCase", "addAudioMedia: ${resultTimedValue.duration}"))

        resultTimedValue.value
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
    }.also {
        downloadAttemptUrlList -= downloadUrl
    }
}

class AlreadyDownloadingMedia(
    override val message: String = "같은 출처의 미디어가 현재 다운로드중입니다."
) : Throwable()

class AlreadyDownloadedMedia(
    override val message: String = "이미 저장된 미디어입니다."
) : Throwable()