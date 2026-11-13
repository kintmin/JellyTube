package com.kintmin.domain.audio_media.usecase

import com.kintmin.domain.app_setting.usecase.FetchPlaylistIdOnDownloadFlowUseCase
import com.kintmin.domain.app_setting.usecase.FetchShouldInsertAtTopOnDownloadFlowUseCase
import com.kintmin.domain.audio_media.model.AlreadyDownloadedMedia
import com.kintmin.domain.audio_media.model.AlreadyDownloadingMedia
import com.kintmin.domain.audio_media.model.AudioMedia
import com.kintmin.domain.audio_media.model.DownloadedAudioMediaResult
import com.kintmin.domain.audio_media.model.DownloadedMedia
import com.kintmin.domain.audio_media.repository.AudioMediaRepository
import com.kintmin.domain.device.repository.DeviceStatusRepository
import com.kintmin.log.AppLog
import com.kintmin.log.model.DebugLog
import com.kintmin.log.model.FirebaseEvent
import kotlinx.coroutines.flow.first
import kotlin.time.measureTimedValue

class DownloadAudioMediaUseCase constructor(
    private val audioMediaRepository: AudioMediaRepository,
    private val deviceStatusRepository: DeviceStatusRepository,
    private val fetchShouldInsertAtTopOnDownloadFlowUseCase: FetchShouldInsertAtTopOnDownloadFlowUseCase,
    private val fetchPlaylistIdOnDownloadFlowUseCase: FetchPlaylistIdOnDownloadFlowUseCase,
    private val appLog: AppLog,
) {
    suspend operator fun invoke(downloadUrl: String): Result<DownloadedAudioMediaResult> = runCatching {
        if (!audioMediaRepository.reserveDownload(downloadUrl)) throw AlreadyDownloadingMedia()
        try {
            download(downloadUrl)
        } finally {
            audioMediaRepository.releaseDownload(downloadUrl)
        }
    }.onFailure { exception ->
        val systemMemory = deviceStatusRepository.getSystemMemory().getOrNull()
        val connectionStatus = deviceStatusRepository.getConnectionStatus().getOrNull()

        appLog.sendFirebaseEvent(
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

    private suspend fun download(downloadUrl: String): DownloadedAudioMediaResult {
        audioMediaRepository.getAudioMediaBySource(source = downloadUrl).onSuccess {
            throw AlreadyDownloadedMedia()
        }

        val downloadedAudioMediaTimedValue = measureTimedValue { downloadWithThumbnail(downloadUrl) }
        appLog.sendDebugLog(DebugLog("DownloadAudioMediaUseCase", "downloadAudioMedia: ${downloadedAudioMediaTimedValue.duration}"))

        val downloadedAudioMedia = downloadedAudioMediaTimedValue.value

        val shouldInsertAtTopOnDownload = fetchShouldInsertAtTopOnDownloadFlowUseCase().first()
        val playlistIdOnDownload = fetchPlaylistIdOnDownloadFlowUseCase().first()

        // 대상 플레이리스트 해석(유효성 검사·미분류 fallback)과 시스템 플레이리스트 보장은 데이터 계층이 담당한다.
        val added = audioMediaRepository.addAudioMedia(
            downloadedAudioMedia = downloadedAudioMedia,
            playlistIdOnDownload = playlistIdOnDownload,
            shouldInsertAtTopOnDownload = shouldInsertAtTopOnDownload,
        ).onFailure {
            audioMediaRepository.deleteDownloadedFile(downloadedAudioMedia)
        }.getOrThrow()

        appLog.sendFirebaseEvent(FirebaseEvent.AddAudioMedia(downloadUrl, added.totalPlaylistMediaCount))
        return DownloadedAudioMediaResult(
            audioMedia = added.audioMedia,
            playlistIdOnDownload = added.resolvedPlaylistIdOnDownload,
            totalPlaylistId = added.totalPlaylistId,
            shouldInsertAtTopOnDownload = shouldInsertAtTopOnDownload,
        )
    }

    // 썸네일은 없어도 곡은 들일 수 있어 실패를 무시한다.
    private suspend fun downloadWithThumbnail(downloadUrl: String): DownloadedMedia {
        val audio = audioMediaRepository.downloadYoutubeAudio(downloadUrl).getOrThrow()
        return DownloadedMedia(
            downloadUrl = downloadUrl,
            title = audio.title,
            audioFileNameWithExt = audio.audioFileNameWithExt,
            imageFileNameWithExt = audioMediaRepository.saveThumbnail(audio.thumbnailUrl, audio.fileName).getOrNull(),
            duration = audio.duration,
            uploader = audio.uploader,
            description = audio.description,
        )
    }
}
