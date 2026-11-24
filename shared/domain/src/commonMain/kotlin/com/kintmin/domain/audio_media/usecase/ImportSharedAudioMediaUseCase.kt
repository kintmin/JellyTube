package com.kintmin.domain.audio_media.usecase

import com.kintmin.domain.app_setting.usecase.FetchPlaylistIdOnDownloadFlowUseCase
import com.kintmin.domain.app_setting.usecase.FetchShouldInsertAtTopOnDownloadFlowUseCase
import com.kintmin.domain.audio_media.model.AudioMedia
import com.kintmin.domain.audio_media.repository.AudioMediaRepository
import com.kintmin.domain.common.suspendRunCatching
import com.kintmin.domain.playlist.repository.PlaylistRepository
import com.kintmin.domain.secret_mode.isSecretTarget
import com.kintmin.log.AppLog
import com.kintmin.log.model.DebugLog
import com.kintmin.log.model.ErrorLog
import com.kintmin.log.secret
import kotlinx.coroutines.flow.first

class ImportSharedAudioMediaUseCase constructor(
    private val audioMediaRepository: AudioMediaRepository,
    private val fetchShouldInsertAtTopOnDownloadFlowUseCase: FetchShouldInsertAtTopOnDownloadFlowUseCase,
    private val fetchPlaylistIdOnDownloadFlowUseCase: FetchPlaylistIdOnDownloadFlowUseCase,
    private val playlistRepository: PlaylistRepository,
    private val appLog: AppLog,
) {

    suspend operator fun invoke(
        contentUriString: String,
        playlistIdOverride: Int? = null,
    ): Result<AudioMedia> {
        // 담을 곳을 확인하기 전에 실패하면 비밀 곡일 수 있어 가린다.
        var isSecret = true
        return suspendRunCatching {
            val playlistId = playlistIdOverride ?: fetchPlaylistIdOnDownloadFlowUseCase().first()
            isSecret = playlistRepository.isSecretTarget(playlistId, appLog)
            audioMediaRepository.importSharedAudio(
                contentUriString = contentUriString,
                playlistIdOnDownload = playlistId,
                shouldInsertAtTopOnDownload = fetchShouldInsertAtTopOnDownloadFlowUseCase().first(),
            ).getOrThrow().audioMedia
        }
            .onSuccess { log("들여옴: id=${it.id} name=${secret(it.name, isSecret)}") }
            .onFailure { appLog.sendErrorLog(ErrorLog(LOG_TAG, "들여오기 실패: ${secret(contentUriString, isSecret)}", it)) }
    }

    private fun log(message: String) = appLog.sendDebugLog(DebugLog(LOG_TAG, message))
}

private const val LOG_TAG = "ImportSharedAudio"
