package com.kintmin.domain.audio_media.usecase

import com.kintmin.domain.app_setting.usecase.FetchShouldInsertAtTopOnDownloadFlowUseCase
import com.kintmin.domain.audio_media.model.AlreadyDownloadedMedia
import com.kintmin.domain.audio_media.model.AlreadyDownloadingMedia
import com.kintmin.domain.audio_media.model.AudioMedia
import com.kintmin.domain.audio_media.model.DownloadedAudioMediaResult
import com.kintmin.domain.audio_media.model.DownloadedMedia
import com.kintmin.domain.audio_media.model.DownloadedYoutubeAudio
import com.kintmin.domain.audio_media.repository.AudioMediaRepository
import com.kintmin.domain.common.suspendRunCatching
import com.kintmin.domain.device.repository.DeviceStatusRepository
import com.kintmin.domain.playlist.isSecretTarget
import com.kintmin.domain.playlist.repository.PlaylistRepository
import com.kintmin.log.AppLog
import com.kintmin.log.model.DebugLog
import com.kintmin.log.model.SentryEvent
import com.kintmin.log.secret
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext
import kotlin.time.measureTimedValue

class DownloadAudioMediaUseCase constructor(
    private val audioMediaRepository: AudioMediaRepository,
    private val playlistRepository: PlaylistRepository,
    private val deviceStatusRepository: DeviceStatusRepository,
    private val fetchShouldInsertAtTopOnDownloadFlowUseCase: FetchShouldInsertAtTopOnDownloadFlowUseCase,
    private val appLog: AppLog,
) {
    /** [intoUncategorized] 면 다운로드 설정을 따르지 않고 미분류 맨 뒤에 넣는다. 담을 자리는 호출자가 정한다. */
    suspend operator fun invoke(
        downloadUrl: String,
        intoUncategorized: Boolean = false,
        playlistIdOnDownload: Int? = null,
    ): Result<DownloadedAudioMediaResult> {
        val isSecret = !intoUncategorized && playlistRepository.isSecretTarget(playlistIdOnDownload)
        return suspendRunCatching {
            appLog.sendDebugLog(DebugLog("DownloadAudioMediaUseCase", "받기 시작: ${secret(downloadUrl, isSecret)}"))
            if (!audioMediaRepository.reserveDownload(downloadUrl)) throw AlreadyDownloadingMedia()
            try {
                download(downloadUrl, playlistIdOnDownload, intoUncategorized, isSecret)
            } finally {
                audioMediaRepository.releaseDownload(downloadUrl)
            }
        }.onFailure { exception -> logFailure(downloadUrl, isSecret, exception) }
    }

    private suspend fun logFailure(downloadUrl: String, isSecret: Boolean, exception: Throwable) {
        if (exception is AlreadyDownloadedMedia || exception is AlreadyDownloadingMedia) {
            appLog.sendDebugLog(DebugLog("DownloadAudioMediaUseCase", "이미 있거나 받는 중이라 받지 않음: ${secret(downloadUrl, isSecret)}"))
            return
        }
        val systemMemory = deviceStatusRepository.getSystemMemory().getOrNull()
        val connectionStatus = deviceStatusRepository.getConnectionStatus().getOrNull()

        appLog.sendSentryEvent(
            SentryEvent.FailedDownloadAudioMedia(
                source = secret(downloadUrl, isSecret),
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

    private suspend fun download(
        downloadUrl: String,
        playlistIdOnDownload: Int?,
        intoUncategorized: Boolean,
        isSecret: Boolean,
    ): DownloadedAudioMediaResult {
        audioMediaRepository.getAudioMediaBySource(source = downloadUrl).onSuccess {
            throw AlreadyDownloadedMedia()
        }

        val audioTimedValue = measureTimedValue { audioMediaRepository.downloadYoutubeAudio(downloadUrl).getOrThrow() }
        appLog.sendDebugLog(DebugLog("DownloadAudioMediaUseCase", "downloadAudioMedia: ${audioTimedValue.duration}"))

        // 음원을 받은 뒤에는 행을 만들 때까지 끊지 않는다. 끊으면 어디에도 연결되지 않은 파일만 남는다.
        return withContext(NonCancellable) {
            addDownloaded(downloadUrl, audioTimedValue.value, playlistIdOnDownload, intoUncategorized, isSecret)
        }
    }

    private suspend fun addDownloaded(
        downloadUrl: String,
        audio: DownloadedYoutubeAudio,
        playlistIdOnDownload: Int?,
        intoUncategorized: Boolean,
        isSecret: Boolean,
    ): DownloadedAudioMediaResult {
        val downloadedAudioMedia = withThumbnail(downloadUrl, audio)

        val shouldInsertAtTopOnDownload = !intoUncategorized && fetchShouldInsertAtTopOnDownloadFlowUseCase().first()
        val resolvedPlaylistId = if (intoUncategorized) null else playlistIdOnDownload

        val added = audioMediaRepository.addAudioMedia(
            downloadedAudioMedia = downloadedAudioMedia,
            playlistIdOnDownload = resolvedPlaylistId,
            shouldInsertAtTopOnDownload = shouldInsertAtTopOnDownload,
        ).onFailure {
            audioMediaRepository.deleteDownloadedFile(downloadedAudioMedia)
        }.getOrThrow()

        appLog.sendSentryEvent(
            SentryEvent.AddAudioMedia(
                source = secret(downloadUrl, isSecret),
                audioMediaCount = added.totalPlaylistMediaCount,
                audioMediaId = added.audioMedia.id,
                audioMediaName = secret(added.audioMedia.name, isSecret),
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
