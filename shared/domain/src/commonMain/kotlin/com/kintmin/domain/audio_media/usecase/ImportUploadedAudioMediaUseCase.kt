package com.kintmin.domain.audio_media.usecase

import com.kintmin.domain.app_setting.usecase.FetchPlaylistIdOnDownloadFlowUseCase
import com.kintmin.domain.app_setting.usecase.FetchShouldInsertAtTopOnDownloadFlowUseCase
import com.kintmin.domain.audio_media.model.ImportedAudioMediaResult
import com.kintmin.domain.audio_media.model.UploadedAudioFile
import com.kintmin.domain.audio_media.repository.AudioMediaRepository
import kotlinx.coroutines.flow.first

/** 받은 순서 그대로 목록에 놓는다. 결과는 [uploads] 와 같은 순서이고, 한 곡의 실패가 뒤 곡을 막지 않는다. */
class ImportUploadedAudioMediaUseCase constructor(
    private val audioMediaRepository: AudioMediaRepository,
    private val fetchShouldInsertAtTopOnDownloadFlowUseCase: FetchShouldInsertAtTopOnDownloadFlowUseCase,
    private val fetchPlaylistIdOnDownloadFlowUseCase: FetchPlaylistIdOnDownloadFlowUseCase,
) {
    suspend operator fun invoke(uploads: List<UploadedAudioFile>): List<Result<ImportedAudioMediaResult>> {
        val shouldInsertAtTop = fetchShouldInsertAtTopOnDownloadFlowUseCase().first()
        val playlistId = fetchPlaylistIdOnDownloadFlowUseCase().first()

        // 맨 위에 넣을 때는 한 곡씩 위로 쌓이므로 뒤에서부터 넣어야 묶음이 요청 순서로 놓인다.
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
