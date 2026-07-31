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
    /**
     * [CreateUploadedAudioStagingFileUseCase]로 발급받은 [stagingFilePath]에 수신이 끝난 파일을 등록한다.
     * [sha256Hex]는 수신 스트림에서 계산한 값이며 중복 판정 키로 쓰인다.
     */
    suspend operator fun invoke(
        stagingFilePath: String,
        sha256Hex: String,
        originalFileName: String,
    ): Result<ImportedAudioMediaResult> = runCatching {
        val shouldInsertAtTop = fetchShouldInsertAtTopOnDownloadFlowUseCase().first()
        val playlistId = fetchPlaylistIdOnDownloadFlowUseCase().first()

        // 대상 해석과 시스템 플레이리스트 보장은 데이터 계층이 담당한다.
        val added = audioMediaRepository.importUploadedAudio(
            stagingFilePath = stagingFilePath,
            sha256Hex = sha256Hex,
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
