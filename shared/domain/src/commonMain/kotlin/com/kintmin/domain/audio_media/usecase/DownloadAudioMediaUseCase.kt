package com.kintmin.domain.audio_media.usecase

import com.kintmin.domain.app_setting.usecase.FetchShouldInsertAtTopOnDownloadFlowUseCase
import com.kintmin.domain.audio_media.model.AlreadyDownloadedMedia
import com.kintmin.domain.audio_media.model.AlreadyDownloadingMedia
import com.kintmin.domain.audio_media.model.AudioMedia
import com.kintmin.domain.audio_media.model.DownloadedAudioMediaResult
import com.kintmin.domain.audio_media.model.DownloadedMedia
import com.kintmin.domain.audio_media.model.DownloadedYoutubeAudio
import com.kintmin.domain.audio_media.repository.AudioMediaRepository
import com.kintmin.domain.audio_track.repository.AudioTrackRepository
import com.kintmin.domain.common.suspendRunCatching
import com.kintmin.domain.device.repository.DeviceStatusRepository
import com.kintmin.domain.playlist.repository.PlaylistRepository
import com.kintmin.domain.secret_mode.isSecretTarget
import com.kintmin.domain.secret_mode.secretAudioMediaJudge
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
    private val audioTrackRepository: AudioTrackRepository,
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
        val isSecret = !intoUncategorized && playlistRepository.isSecretTarget(playlistIdOnDownload, appLog) ||
            isOwnedSecret(downloadUrl)
        return suspendRunCatching {
            if (!audioMediaRepository.reserveDownload(downloadUrl)) throw AlreadyDownloadingMedia()
            try {
                appLog.sendDebugLog(DebugLog("DownloadAudioMediaUseCase", "받기 시작: ${secret(downloadUrl, isSecret)}"))
                download(downloadUrl, playlistIdOnDownload, intoUncategorized, isSecret)
            } finally {
                audioMediaRepository.releaseDownload(downloadUrl)
            }
        }.onFailure { exception -> logFailure(downloadUrl, isSecret, exception) }
    }

    // 같은 주소가 이미 비밀 곡이면 공개 대상으로 받으려 해도 그 주소는 비밀이다.
    private suspend fun isOwnedSecret(downloadUrl: String): Boolean {
        val owned = audioMediaRepository.getAudioMediaBySource(downloadUrl).getOrNull() ?: return false
        return audioTrackRepository.secretAudioMediaJudge(appLog)(owned.id)
    }

    private suspend fun logFailure(downloadUrl: String, isSecret: Boolean, exception: Throwable) {
        if (exception is AlreadyDownloadingMedia) {
            // 먼저 받는 요청이 비밀 대상일 수 있는데 그 대상을 알 수 없어 주소를 싣지 않는다.
            appLog.sendDebugLog(DebugLog("DownloadAudioMediaUseCase", "같은 주소를 받는 중이라 받지 않음"))
            return
        }
        if (exception is AlreadyDownloadedMedia) {
            appLog.sendDebugLog(DebugLog("DownloadAudioMediaUseCase", "이미 있어 받지 않음: ${secret(downloadUrl, isSecret)}"))
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

        val audioTimedValue = measureTimedValue { audioMediaRepository.downloadYoutubeAudio(downloadUrl, isSecret).getOrThrow() }
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
                audioMediaCount = secret(added.totalPlaylistMediaCount, isSecret),
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
