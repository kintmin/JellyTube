package com.kintmin.domain.audio_media.usecase

import com.kintmin.domain.app_setting.usecase.FetchPlaylistIdOnDownloadFlowUseCase
import com.kintmin.domain.app_setting.usecase.FetchShouldInsertAtTopOnDownloadFlowUseCase
import com.kintmin.domain.audio_media.model.AudioMedia
import com.kintmin.domain.audio_media.repository.AudioMediaRepository
import com.kintmin.domain.playlist.model.Playlist
import com.kintmin.domain.playlist.usecase.FetchAllPlaylistFlowUseCase
import kotlinx.coroutines.flow.first

class ImportUploadedAudioMediaUseCase constructor(
    private val audioMediaRepository: AudioMediaRepository,
    private val fetchShouldInsertAtTopOnDownloadFlowUseCase: FetchShouldInsertAtTopOnDownloadFlowUseCase,
    private val fetchPlaylistIdOnDownloadFlowUseCase: FetchPlaylistIdOnDownloadFlowUseCase,
    private val fetchAllPlaylistFlowUseCase: FetchAllPlaylistFlowUseCase,
) {
    suspend operator fun invoke(bytes: ByteArray, originalFileName: String): Result<ImportedAudioMediaResult> = runCatching {
        val shouldInsertAtTop = fetchShouldInsertAtTopOnDownloadFlowUseCase().first()
        val playlistId = fetchPlaylistIdOnDownloadFlowUseCase().first()
        val allPlaylistIdSet = fetchAllPlaylistFlowUseCase().first().map { it.id }.toSet() +
            setOf(Playlist.TOTAL, Playlist.UNCATEGORIZED)
        val resolvedPlaylistId = if (playlistId in allPlaylistIdSet) playlistId else Playlist.UNCATEGORIZED

        val (audioMedia, _) = audioMediaRepository.importUploadedAudio(
            bytes = bytes,
            originalFileName = originalFileName,
            playlistIdOnDownload = resolvedPlaylistId,
            shouldInsertAtTopOnDownload = shouldInsertAtTop,
        ).getOrThrow()

        ImportedAudioMediaResult(
            audioMedia = audioMedia,
            playlistIdOnDownload = resolvedPlaylistId,
        )
    }
}

data class ImportedAudioMediaResult(
    val audioMedia: AudioMedia,
    val playlistIdOnDownload: Int,
)
