package com.kintmin.domain.audio_media.usecase

import com.kintmin.domain.app_setting.usecase.FetchPlaylistIdOnDownloadFlowUseCase
import com.kintmin.domain.app_setting.usecase.FetchShouldInsertAtTopOnDownloadFlowUseCase
import com.kintmin.domain.audio_media.model.AudioMedia
import com.kintmin.domain.audio_media.repository.AudioMediaRepository
import com.kintmin.log.AppLog
import com.kintmin.log.model.DebugLog
import com.kintmin.log.model.ErrorLog
import kotlinx.coroutines.flow.first

class ImportSharedAudioMediaUseCase constructor(
    private val audioMediaRepository: AudioMediaRepository,
    private val fetchShouldInsertAtTopOnDownloadFlowUseCase: FetchShouldInsertAtTopOnDownloadFlowUseCase,
    private val fetchPlaylistIdOnDownloadFlowUseCase: FetchPlaylistIdOnDownloadFlowUseCase,
    private val appLog: AppLog,
) {

    suspend operator fun invoke(
        contentUriString: String,
        playlistIdOverride: Int? = null,
    ): Result<AudioMedia> = runCatching {
        val shouldInsertAtTop = fetchShouldInsertAtTopOnDownloadFlowUseCase().first()
        val playlistId = playlistIdOverride ?: fetchPlaylistIdOnDownloadFlowUseCase().first()

        audioMediaRepository.importSharedAudio(
            contentUriString = contentUriString,
            playlistIdOnDownload = playlistId,
            shouldInsertAtTopOnDownload = shouldInsertAtTop,
        ).getOrThrow().audioMedia
    }
        .onSuccess { log("들여옴: id=${it.id} name=${it.name}") }
        .onFailure { appLog.sendErrorLog(ErrorLog(LOG_TAG, "들여오기 실패: $contentUriString", it)) }

    private fun log(message: String) = appLog.sendDebugLog(DebugLog(LOG_TAG, message))
}

private const val LOG_TAG = "ImportSharedAudio"
