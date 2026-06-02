package com.kintmin.domain.audio_media.usecase

import com.kintmin.domain.audio_media.model.AudioMedia
import com.kintmin.domain.audio_media.repository.AudioMediaRepository
import com.kintmin.domain.app_setting.usecase.FetchPlaylistIdOnDownloadFlowUseCase
import com.kintmin.domain.app_setting.usecase.FetchShouldInsertAtTopOnDownloadFlowUseCase
import com.kintmin.domain.device.repository.DeviceStatusRepository
import com.kintmin.domain.playlist.model.Playlist
import com.kintmin.domain.playlist.usecase.FetchAllPlaylistFlowUseCase
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
    private val fetchAllPlaylistFlowUseCase: FetchAllPlaylistFlowUseCase,
    private val appLog: AppLog,
) {
    private var downloadAttemptUrlList = mutableSetOf<String>()

    suspend operator fun invoke(downloadUrl: String): Result<DownloadedAudioMediaResult> = runCatching {
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
        appLog.sendDebugLog(DebugLog("DownloadAudioMediaUseCase", "downloadAudioMedia: ${downloadedAudioMediaTimedValue.duration}"))

        val downloadedAudioMedia = downloadedAudioMediaTimedValue.value

        val shouldInsertAtTopOnDownload = fetchShouldInsertAtTopOnDownloadFlowUseCase().first()
        val playlistIdOnDownload = fetchPlaylistIdOnDownloadFlowUseCase().first()
        val allPlaylistIdSet = fetchAllPlaylistFlowUseCase().first().map { it.id }.toSet() + setOf(
            Playlist.TOTAL,
            Playlist.UNCATEGORIZED,
        )
        val resolvedPlaylistIdOnDownload = if (playlistIdOnDownload in allPlaylistIdSet) {
            playlistIdOnDownload
        } else {
            Playlist.UNCATEGORIZED
        }

        val (audioMedia, totalPlaylistMediaCount) = audioMediaRepository.addAudioMedia(
            downloadedAudioMedia = downloadedAudioMedia,
            playlistIdOnDownload = resolvedPlaylistIdOnDownload,
            shouldInsertAtTopOnDownload = shouldInsertAtTopOnDownload,
        ).onFailure {
            audioMediaRepository.deleteDownloadedFile(downloadedAudioMedia)
        }.getOrThrow()

        appLog.sendFirebaseEvent(FirebaseEvent.AddAudioMedia(downloadUrl, totalPlaylistMediaCount))
        DownloadedAudioMediaResult(
            audioMedia = audioMedia,
            playlistIdOnDownload = resolvedPlaylistIdOnDownload,
            shouldInsertAtTopOnDownload = shouldInsertAtTopOnDownload,
        )
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

data class DownloadedAudioMediaResult(
    val audioMedia: AudioMedia,
    val playlistIdOnDownload: Int,
    val shouldInsertAtTopOnDownload: Boolean,
)
