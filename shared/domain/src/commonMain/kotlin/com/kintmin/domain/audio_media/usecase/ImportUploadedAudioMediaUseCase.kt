package com.kintmin.domain.audio_media.usecase

import com.kintmin.domain.app_setting.usecase.FetchPlaylistIdOnDownloadFlowUseCase
import com.kintmin.domain.app_setting.usecase.FetchShouldInsertAtTopOnDownloadFlowUseCase
import com.kintmin.domain.audio_media.model.AudioMedia
import com.kintmin.domain.audio_media.repository.AudioMediaRepository
import kotlinx.coroutines.flow.first

class ImportUploadedAudioMediaUseCase constructor(
    private val audioMediaRepository: AudioMediaRepository,
    private val fetchShouldInsertAtTopOnDownloadFlowUseCase: FetchShouldInsertAtTopOnDownloadFlowUseCase,
    private val fetchPlaylistIdOnDownloadFlowUseCase: FetchPlaylistIdOnDownloadFlowUseCase,
) {
    suspend operator fun invoke(bytes: ByteArray, originalFileName: String): Result<ImportedAudioMediaResult> = runCatching {
        val shouldInsertAtTop = fetchShouldInsertAtTopOnDownloadFlowUseCase().first()
        val playlistId = fetchPlaylistIdOnDownloadFlowUseCase().first()

        // 대상 해석과 시스템 플레이리스트 보장은 데이터 계층이 담당한다.
        val added = audioMediaRepository.importUploadedAudio(
            bytes = bytes,
            originalFileName = originalFileName,
            playlistIdOnDownload = playlistId,
            shouldInsertAtTopOnDownload = shouldInsertAtTop,
        ).getOrThrow()

        ImportedAudioMediaResult(
            audioMedia = added.audioMedia,
            playlistIdOnDownload = added.resolvedPlaylistIdOnDownload,
        )
    }
}

data class ImportedAudioMediaResult(
    val audioMedia: AudioMedia,
    val playlistIdOnDownload: Int,
)
