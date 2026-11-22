package com.kintmin.domain.audio_media.usecase

import com.kintmin.domain.app_setting.usecase.FetchPlaylistIdOnDownloadFlowUseCase
import com.kintmin.domain.app_setting.usecase.FetchShouldInsertAtTopOnDownloadFlowUseCase
import com.kintmin.domain.audio_media.model.AlreadyDownloadedMedia
import com.kintmin.domain.audio_media.model.AlreadyDownloadingMedia
import com.kintmin.domain.audio_media.model.AudioMedia
import com.kintmin.domain.audio_media.model.DownloadedAudioMediaResult
import com.kintmin.domain.audio_media.model.DownloadedMedia
import com.kintmin.domain.audio_media.model.DownloadedYoutubeAudio
import com.kintmin.domain.audio_media.repository.AudioMediaRepository
import com.kintmin.domain.device.repository.DeviceStatusRepository
import com.kintmin.log.AppLog
import com.kintmin.log.model.DebugLog
import com.kintmin.log.model.SentryEvent
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext
import kotlin.coroutines.cancellation.CancellationException
import kotlin.time.measureTimedValue

class DownloadAudioMediaUseCase constructor(
    private val audioMediaRepository: AudioMediaRepository,
    private val deviceStatusRepository: DeviceStatusRepository,
    private val fetchShouldInsertAtTopOnDownloadFlowUseCase: FetchShouldInsertAtTopOnDownloadFlowUseCase,
    private val fetchPlaylistIdOnDownloadFlowUseCase: FetchPlaylistIdOnDownloadFlowUseCase,
    private val appLog: AppLog,
) {
    /** [intoUncategorized] 면 다운로드 설정을 따르지 않고 미분류 맨 뒤에 넣는다. 담을 자리는 호출자가 정한다. */
    suspend operator fun invoke(
        downloadUrl: String,
        intoUncategorized: Boolean = false,
    ): Result<DownloadedAudioMediaResult> = runCatching {
        appLog.sendDebugLog(DebugLog("DownloadAudioMediaUseCase", "받기 시작: $downloadUrl"))
        if (!audioMediaRepository.reserveDownload(downloadUrl)) throw AlreadyDownloadingMedia()
        try {
            download(downloadUrl, intoUncategorized)
        } finally {
            audioMediaRepository.releaseDownload(downloadUrl)
        }
    }.onFailure { exception ->
        // 멈추기·나가기로 끊긴 것은 받기 실패가 아니다.
        if (exception is CancellationException) return@onFailure
        if (exception is AlreadyDownloadedMedia || exception is AlreadyDownloadingMedia) {
            appLog.sendDebugLog(DebugLog("DownloadAudioMediaUseCase", "이미 있거나 받는 중이라 받지 않음: $downloadUrl"))
            return@onFailure
        }
        val systemMemory = deviceStatusRepository.getSystemMemory().getOrNull()
        val connectionStatus = deviceStatusRepository.getConnectionStatus().getOrNull()

        appLog.sendSentryEvent(
            SentryEvent.FailedDownloadAudioMedia(
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

    private suspend fun download(downloadUrl: String, intoUncategorized: Boolean): DownloadedAudioMediaResult {
        audioMediaRepository.getAudioMediaBySource(source = downloadUrl).onSuccess {
            throw AlreadyDownloadedMedia()
        }

        val audioTimedValue = measureTimedValue { audioMediaRepository.downloadYoutubeAudio(downloadUrl).getOrThrow() }
        appLog.sendDebugLog(DebugLog("DownloadAudioMediaUseCase", "downloadAudioMedia: ${audioTimedValue.duration}"))

        // 음원을 받은 뒤에는 행을 만들 때까지 끊지 않는다. 끊으면 어디에도 연결되지 않은 파일만 남는다.
        return withContext(NonCancellable) { addDownloaded(downloadUrl, audioTimedValue.value, intoUncategorized) }
    }

    private suspend fun addDownloaded(
        downloadUrl: String,
        audio: DownloadedYoutubeAudio,
        intoUncategorized: Boolean,
    ): DownloadedAudioMediaResult {
        val downloadedAudioMedia = withThumbnail(downloadUrl, audio)

        val shouldInsertAtTopOnDownload = !intoUncategorized && fetchShouldInsertAtTopOnDownloadFlowUseCase().first()
        val playlistIdOnDownload = if (intoUncategorized) null else fetchPlaylistIdOnDownloadFlowUseCase().first()

        val added = audioMediaRepository.addAudioMedia(
            downloadedAudioMedia = downloadedAudioMedia,
            playlistIdOnDownload = playlistIdOnDownload,
            shouldInsertAtTopOnDownload = shouldInsertAtTopOnDownload,
        ).onFailure {
            audioMediaRepository.deleteDownloadedFile(downloadedAudioMedia)
        }.getOrThrow()

        appLog.sendSentryEvent(
            SentryEvent.AddAudioMedia(
                source = downloadUrl,
                audioMediaCount = added.totalPlaylistMediaCount,
                audioMediaId = added.audioMedia.id,
                audioMediaName = added.audioMedia.name,
                playlistId = added.resolvedPlaylistIdOnDownload,
            ),
        )
        return DownloadedAudioMediaResult(
            audioMedia = added.audioMedia,
            playlistIdOnDownload = added.resolvedPlaylistIdOnDownload,
            totalPlaylistId = added.totalPlaylistId,
            shouldInsertAtTopOnDownload = shouldInsertAtTopOnDownload,
        )
    }

    // 썸네일은 없어도 곡은 들일 수 있어 실패를 무시한다.
    private suspend fun withThumbnail(downloadUrl: String, audio: DownloadedYoutubeAudio): DownloadedMedia {
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
