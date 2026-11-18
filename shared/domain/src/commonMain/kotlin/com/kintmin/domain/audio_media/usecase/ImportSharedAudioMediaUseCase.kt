package com.kintmin.domain.audio_media.usecase

import com.kintmin.domain.app_setting.usecase.FetchPlaylistIdOnDownloadFlowUseCase
import com.kintmin.domain.app_setting.usecase.FetchShouldInsertAtTopOnDownloadFlowUseCase
import com.kintmin.domain.audio_media.model.AudioMedia
import com.kintmin.domain.audio_media.repository.AudioMediaRepository
import kotlinx.coroutines.flow.first

class ImportSharedAudioMediaUseCase constructor(
    private val audioMediaRepository: AudioMediaRepository,
    private val fetchShouldInsertAtTopOnDownloadFlowUseCase: FetchShouldInsertAtTopOnDownloadFlowUseCase,
    private val fetchPlaylistIdOnDownloadFlowUseCase: FetchPlaylistIdOnDownloadFlowUseCase,
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
}
