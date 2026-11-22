package com.kintmin.domain.audio_media.usecase

import com.kintmin.domain.app_setting.usecase.FetchPlaylistIdOnDownloadFlowUseCase
import com.kintmin.domain.app_setting.usecase.FetchShouldInsertAtTopOnDownloadFlowUseCase
import com.kintmin.domain.audio_media.model.ImportedAudioMediaResult
import com.kintmin.domain.audio_media.model.UploadedAudioFile
import com.kintmin.domain.audio_media.repository.AudioMediaRepository
import kotlinx.coroutines.flow.first

class ImportUploadedAudioMediaUseCase constructor(
    private val audioMediaRepository: AudioMediaRepository,
    private val fetchShouldInsertAtTopOnDownloadFlowUseCase: FetchShouldInsertAtTopOnDownloadFlowUseCase,
    private val fetchPlaylistIdOnDownloadFlowUseCase: FetchPlaylistIdOnDownloadFlowUseCase,
) {
    suspend operator fun invoke(uploads: List<UploadedAudioFile>): List<Result<ImportedAudioMediaResult>> {
        val shouldInsertAtTop = fetchShouldInsertAtTopOnDownloadFlowUseCase().first()
        val playlistId = fetchPlaylistIdOnDownloadFlowUseCase().first()

        val insertionOrder = if (shouldInsertAtTop) uploads.indices.reversed() else uploads.indices
        val resultByIndex = insertionOrder.associateWith { index ->
            val upload = uploads[index]
            audioMediaRepository.importUploadedAudio(
                uploadedFileFullPath = upload.fileFullPath,
                originalFileName = upload.originalFileName,
                playlistIdOnDownload = playlistId,
                shouldInsertAtTopOnDownload = shouldInsertAtTop,
            ).map { added ->
                ImportedAudioMediaResult(
                    audioMedia = added.audioMedia,
                    playlistIdOnDownload = added.resolvedPlaylistIdOnDownload,
                )
            }
        }
        return uploads.indices.map(resultByIndex::getValue)
    }
}
